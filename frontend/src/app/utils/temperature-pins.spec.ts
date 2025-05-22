import { createTemperaturePinSvg } from './temperature-pins';

describe('createTemperaturePinSvg', () => {
  it('should return SVG with blue fill for temperature <= 10', () => {
    const svg = createTemperaturePinSvg(5);
    expect(svg).toContain('#2196F3'); // Blue
    expect(svg).toContain('5°C');
  });

  it('should return SVG with orange fill for temperature between 11 and 30', () => {
    const svg = createTemperaturePinSvg(25);
    expect(svg).toContain('#FB8C00'); // Orange
    expect(svg).toContain('25°C');
  });

  it('should return SVG with red fill for temperature > 30', () => {
    const svg = createTemperaturePinSvg(35);
    expect(svg).toContain('#E53935'); // Red
    expect(svg).toContain('35°C');
  });

  it('should use thick stroke when selected is true', () => {
    const svg = createTemperaturePinSvg(20, true);
    expect(svg).toContain('stroke="#000"');
    expect(svg).toContain('stroke-width="4"');
  });

  it('should use default stroke when selected is false', () => {
    const svg = createTemperaturePinSvg(20);
    expect(svg).toContain('stroke="#333"');
    expect(svg).toContain('stroke-width="1.5"');
  });

  it('should round temperature value in text', () => {
    const svg = createTemperaturePinSvg(19.6);
    expect(svg).toContain('20°C');
  });
});