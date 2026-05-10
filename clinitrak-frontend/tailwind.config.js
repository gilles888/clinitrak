/** @type {import('tailwindcss').Config} */
module.exports = {
  // Scan uniquement les fichiers Angular pour purger les classes non utilisées en prod
  content: [
    "./src/**/*.{html,ts}",
  ],
  // Préfixe 'tw-' pour éviter les conflits avec PrimeNG
  prefix: 'tw-',
  darkMode: 'class',
  theme: {
    extend: {
      // Palette de couleurs CliniTrak
      colors: {
        primary: {
          50:  '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
          950: '#172554',
        },
        clinical: {
          // Couleurs spécifiques à l'interface médicale
          success:  '#22c55e',
          warning:  '#f59e0b',
          danger:   '#ef4444',
          info:     '#3b82f6',
          neutral:  '#6b7280',
        },
      },
      // Typographie
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      // Espacement pour les composants médicaux
      spacing: {
        '18': '4.5rem',
        '22': '5.5rem',
        '72': '18rem',
        '84': '21rem',
        '96': '24rem',
      },
      // Hauteur minimale de la sidebar
      minHeight: {
        'sidebar': 'calc(100vh - 4rem)',
      },
    },
  },
  plugins: [],
}
