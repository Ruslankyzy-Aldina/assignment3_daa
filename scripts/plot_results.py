import csv
import json
import math
from decimal import Decimal
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "results" / "plots"
ALGORITHMS = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
COLORS = ["black", "0.4", "0.6", "black", "0.4"]
MARKERS = ["o", "s", "^", "o", "x"]


def load_rows():
    with (ROOT / "results" / "results.csv").open(encoding="utf-8-sig") as source:
        rows = list(csv.DictReader(source))
    for row in rows:
        for key in ["n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"]:
            row[key] = int(Decimal(row[key]))
        row["time_ms"] = float(row["time_ms"])
        row["density"] = row["m"] // row["n"]
        row["operations"] = row["dsu_ops"] + row["edge_scans"] + row["comparisons"]
    if len(rows) != 50:
        raise ValueError("Expected 50 benchmark rows")
    return rows


def series(rows, algorithm, density):
    return sorted([r for r in rows if r["algorithm"] == algorithm and r["density"] == density],
                  key=lambda r: r["n"])


def slope(points, key):
    return float(np.polyfit(np.log10([r["n"] for r in points]),
                            np.log10([r[key] for r in points]), 1)[0])


def decorate(ax, title, ylabel):
    ax.set_title(title, fontsize=12, pad=12)
    ax.set_xscale("log")
    ax.set_xlabel("Vertices, n")
    ax.set_ylabel(ylabel)
    ax.grid(True, which="major", color="0.85", linewidth=0.5)
    ax.spines[["top", "right"]].set_visible(False)
    ax.set_axisbelow(True)


def draw_panel(rows, filename, ylabel, value, logarithmic):
    fig, axes = plt.subplots(1, 2, figsize=(10.6, 4.6))
    for ax, density, name in zip(axes, [4, 20], ["Sparse", "Dense"]):
        for algorithm, color, marker in zip(ALGORITHMS, COLORS, MARKERS):
            points = series(rows, algorithm, density)
            ax.plot([r["n"] for r in points], [value(r) for r in points],
                    color=color, marker=marker, markersize=6, linewidth=1.8,
                    markerfacecolor="none" if algorithm == "Kahn" else color,
                    linestyle="--" if algorithm == "DagShortestPath" else "-", label=algorithm)
        decorate(ax, f"{name}: m = {density}n", ylabel)
        if logarithmic:
            ax.set_yscale("log")
        else:
            ax.set_ylim(0, 2.18)
    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="lower center", ncol=5, frameon=False, fontsize=9,
               bbox_to_anchor=(0.5, 0.035))
    fig.text(0.5, 0.012, "Overlapping series: Kahn = DAG relaxation; in scans per edge, Prim = Kosaraju.",
             ha="center", fontsize=8, color="black")
    fig.tight_layout(rect=(0, 0.13, 1, 1))
    fig.savefig(OUTPUT / filename, dpi=230, facecolor="white")
    plt.close(fig)


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    plt.rcParams.update({"font.family": "Times New Roman", "font.size": 11,
                         "axes.labelcolor": "black", "text.color": "black"})
    rows = load_rows()
    draw_panel(rows, "operations_vs_n.png", "Counted operations", lambda r: r["operations"], True)
    draw_panel(rows, "scans_per_edge.png", "Edge scans / m", lambda r: r["edge_scans"] / r["m"], False)
    fig, ax = plt.subplots(figsize=(10.6, 4.5))
    for density, color, marker in [(4, COLORS[0], "o"), (20, COLORS[1], "s")]:
        points = series(rows, "Kruskal", density)
        ax.plot([r["n"] for r in points], [r["edge_scans"] / r["m"] for r in points],
                color=color, marker=marker, linewidth=2, label=f"m = {density}n")
    decorate(ax, "Kruskal: edges examined after sorting", "Edge scans / m")
    ax.set_ylim(0, 1.08)
    ax.legend(frameon=False, loc="lower right")
    fig.tight_layout()
    fig.savefig(OUTPUT / "kruskal_edge_share.png", dpi=230, facecolor="white")
    plt.close(fig)
    analysis = []
    for algorithm in ALGORITHMS:
        for density in [4, 20]:
            points = series(rows, algorithm, density)
            if algorithm in ["Kruskal", "Prim"]:
                ratios = [r["comparisons"] / (r["m"] * math.log2(r["m"])) for r in points]
            else:
                ratios = [r["edge_scans"] / (r["n"] + r["m"]) for r in points]
            analysis.append({"algorithm": algorithm, "density": density,
                             "scan_slope": slope(points, "edge_scans"),
                             "operation_slope": slope(points, "operations"),
                             "ratios": ratios,
                             "dsu_per_edge": [r["dsu_ops"] / r["m"] for r in points],
                             "scan_share": [r["edge_scans"] / r["m"] for r in points]})
    print(json.dumps(analysis, indent=2))


if __name__ == "__main__":
    main()
