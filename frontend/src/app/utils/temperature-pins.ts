export function createTemperaturePinSvg(temp: number, selected = false): string {
  let fillColor = '#2196F3'; // Blue
  if (temp > 10 && temp <= 30) fillColor = '#FB8C00'; // Orange
  else if (temp > 30) fillColor = '#E53935'; // Red

  const strokeColor = selected ? '#000' : '#333';
  const strokeWidth = selected ? 4 : 1.5;

  return `
    <svg xmlns="http://www.w3.org/2000/svg" width="50" height="80" viewBox="0 0 64 96">
      <path d="M32 0C18 0 6.5 11.5 6.5 25.5C6.5 41.7 30.4 76.5 31.2 77.7C31.5 78.1 31.8 78.3 32.1 78.3C32.4 78.3 32.7 78.1 33 77.7C33.8 76.5 57.5 41.7 57.5 25.5C57.5 11.5 46 0 32 0Z"
        fill="${fillColor}" stroke="${strokeColor}" stroke-width="${strokeWidth}"/>
      
      <circle cx="32" cy="25" r="20" fill="white" />

      <text x="32" y="25"
            font-size="18" font-family="Arial, sans-serif"
            fill="${fillColor}" stroke="#000" stroke-width="0.5"
            text-anchor="middle"
            dominant-baseline="middle"
            font-weight="bold">
        ${Math.round(temp)}°C
      </text>
    </svg>
  `.trim();
}