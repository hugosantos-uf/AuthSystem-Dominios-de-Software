interface LogoProps {
  /** "color": hexágonos azuis e texto escuro; "white": tudo em branco, para fundos coloridos. */
  variant?: 'color' | 'white';
  className?: string;
}

// Hexágonos da marca UFG (coordenadas no viewBox original).
const HEXAGONS = [
  '262,231 312,260 312,318 262,347 212,318 212,260',
  '413,174 487,217 487,303 413,346 339,303 339,217',
  '200,326 262,362 262,434 200,470 138,434 138,362',
  '462,332 512,361 512,419 462,448 412,419 412,361',
  '300,455 338,477 338,521 300,543 262,521 262,477',
  '412,434 474,470 474,542 412,578 350,542 350,470',
];

export function Logo({ variant = 'color', className }: LogoProps) {
  const hexFill = variant === 'white' ? '#ffffff' : 'var(--ufg-blue)';
  const textFill = variant === 'white' ? '#ffffff' : '#1d1d1b';
  return (
    <svg className={className} viewBox="120 160 410 600" role="img" aria-label="UFG">
      {HEXAGONS.map((points) => (
        <polygon key={points} points={points} fill={hexFill} />
      ))}
      <text
        x="325"
        y="750"
        textAnchor="middle"
        fill={textFill}
        fontFamily="'Segoe UI', 'Open Sans', Arial, sans-serif"
        fontWeight="700"
        fontSize="170"
        letterSpacing="20"
      >
        UFG
      </text>
    </svg>
  );
}
