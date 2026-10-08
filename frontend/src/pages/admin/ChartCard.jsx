import { useState } from 'react';
import {
  BarElement, CategoryScale, Chart as ChartJS, Filler, LinearScale, LineElement, PointElement, Tooltip,
} from 'chart.js';
import { Bar, Line } from 'react-chartjs-2';
import { useTheme } from '../../context/ThemeContext.jsx';

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, Filler, Tooltip);

/*
 * Chart styling follows one rule set for every admin chart:
 * each chart is a single series, so it uses one colour (no rainbow bars);
 * thin marks with rounded ends; hairline grid; muted axis text; a hover tooltip;
 * and a "Table" toggle so every value is readable without relying on colour or hover.
 * Dark mode uses its own colour steps chosen for the dark surface (not an automatic inversion).
 */
const PALETTE = {
  light: { series: '#2a78d6', fill: 'rgba(42, 120, 214, 0.10)', grid: '#e1e0d9', axis: '#898781', baseline: '#c3c2b7', surface: '#fcfcfb', tooltipBg: '#0b0b0b', tooltipText: '#ffffff' },
  dark: { series: '#3987e5', fill: 'rgba(57, 135, 229, 0.16)', grid: '#2c2c2a', axis: '#898781', baseline: '#383835', surface: '#1a1a19', tooltipBg: '#fcfcfb', tooltipText: '#0b0b0b' },
};
const FONT = { family: "'Outfit', system-ui, sans-serif", size: 12 };

function tooltip(c) {
  return {
    backgroundColor: c.tooltipBg,
    titleColor: c.tooltipText,
    bodyColor: c.tooltipText,
    titleFont: { ...FONT, weight: '600' },
    bodyFont: FONT,
    padding: 10,
    cornerRadius: 8,
    displayColors: false,
  };
}

function barOptions(c, horizontal) {
  const valueAxis = {
    beginAtZero: true,
    ticks: { precision: 0, color: c.axis, font: FONT },
    grid: { color: c.grid, drawTicks: false },
    border: { display: false },
  };
  const categoryAxis = {
    ticks: { color: c.axis, font: FONT },
    grid: { display: false },
    border: { color: c.baseline },
  };
  return {
    indexAxis: horizontal ? 'y' : 'x',
    maintainAspectRatio: false,
    plugins: { legend: { display: false }, tooltip: tooltip(c) },
    scales: horizontal ? { x: valueAxis, y: categoryAxis } : { x: categoryAxis, y: valueAxis },
  };
}

function lineOptions(c) {
  return {
    maintainAspectRatio: false,
    // Crosshair-style hover: anywhere in a month's column shows that month's value
    interaction: { mode: 'index', intersect: false },
    plugins: { legend: { display: false }, tooltip: tooltip(c) },
    scales: {
      x: { ticks: { color: c.axis, font: FONT }, grid: { display: false }, border: { color: c.baseline } },
      y: { beginAtZero: true, ticks: { precision: 0, color: c.axis, font: FONT }, grid: { color: c.grid, drawTicks: false }, border: { display: false } },
    },
  };
}

/**
 * @param type 'line' | 'bar' | 'hbar'
 * @param rows [{ label, count }]
 * @param valueLabel what the numbers mean, e.g. "Applications"
 */
export default function ChartCard({ title, subtitle, type, rows, valueLabel, height = 240 }) {
  const { theme } = useTheme();
  const c = PALETTE[theme] ?? PALETTE.light;
  const [showTable, setShowTable] = useState(false);
  const data = {
    labels: rows.map((r) => r.label),
    datasets: [type === 'line'
      ? {
          label: valueLabel,
          data: rows.map((r) => r.count),
          borderColor: c.series,
          backgroundColor: c.fill,
          borderWidth: 2,
          fill: true,
          // Monotone smoothing never overshoots, so the curve can't suggest values that don't exist
          cubicInterpolationMode: 'monotone',
          pointRadius: 4,
          pointHoverRadius: 6,
          pointBackgroundColor: c.series,
          pointBorderColor: c.surface,
          pointBorderWidth: 2,
        }
      : {
          label: valueLabel,
          data: rows.map((r) => r.count),
          backgroundColor: c.series,
          borderRadius: 4,
          borderSkipped: 'start',
          maxBarThickness: 28,
        }],
  };

  return (
    <section className="panel chart-card h-100">
      <div className="d-flex justify-content-between align-items-start mb-3 gap-2">
        <div>
          <h2 className="h6 fw-bold mb-0">{title}</h2>
          {subtitle && <div className="small text-muted">{subtitle}</div>}
        </div>
        <button type="button" className="btn btn-light btn-sm" onClick={() => setShowTable((v) => !v)}
                aria-pressed={showTable}>
          <i className={`bi bi-${showTable ? 'bar-chart' : 'table'} me-1`} aria-hidden="true" />
          {showTable ? 'Chart' : 'Table'}
        </button>
      </div>

      {showTable ? (
        <table className="table table-sm mb-0 chart-table">
          <thead><tr><th scope="col">{title}</th><th scope="col" className="text-end">{valueLabel}</th></tr></thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.label}><td>{r.label}</td><td className="text-end">{r.count}</td></tr>
            ))}
          </tbody>
        </table>
      ) : (
        <div style={{ height }} role="img" aria-label={`${title}: ${rows.map((r) => `${r.label} ${r.count}`).join(', ')}`}>
          {type === 'line'
            ? <Line key={theme} data={data} options={lineOptions(c)} />
            : <Bar key={theme} data={data} options={barOptions(c, type === 'hbar')} />}
        </div>
      )}
    </section>
  );
}
