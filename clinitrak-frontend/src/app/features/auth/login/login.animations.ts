import { animate, keyframes, state, style, transition, trigger } from '@angular/animations';

/** Animations de la page de connexion CliniTrak. */
export const loginAnimations = [

  trigger('fadeSlideUp', [
    transition(':enter', [
      style({ opacity: 0, transform: 'translateY(28px)' }),
      animate('500ms 80ms cubic-bezier(0.16, 1, 0.3, 1)',
        style({ opacity: 1, transform: 'translateY(0)' }))
    ])
  ]),

  trigger('slideInLeft', [
    transition(':enter', [
      style({ opacity: 0, transform: 'translateX(-24px)' }),
      animate('600ms 120ms cubic-bezier(0.16, 1, 0.3, 1)',
        style({ opacity: 1, transform: 'translateX(0)' }))
    ])
  ]),

  trigger('fadeIn', [
    transition(':enter', [
      style({ opacity: 0, transform: 'translateY(-4px)' }),
      animate('220ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
    ]),
    transition(':leave', [
      animate('160ms ease-in', style({ opacity: 0, transform: 'translateY(-4px)' }))
    ])
  ]),

  trigger('shake', [
    state('idle',   style({})),
    state('active', style({})),
    transition('idle => active', [
      animate('480ms cubic-bezier(0.36, 0.07, 0.19, 0.97)', keyframes([
        style({ transform: 'translateX(0)',    offset: 0 }),
        style({ transform: 'translateX(-9px)', offset: 0.2 }),
        style({ transform: 'translateX(9px)',  offset: 0.4 }),
        style({ transform: 'translateX(-6px)', offset: 0.6 }),
        style({ transform: 'translateX(6px)',  offset: 0.8 }),
        style({ transform: 'translateX(0)',    offset: 1 }),
      ]))
    ])
  ]),

  trigger('successPop', [
    transition(':enter', [
      style({ opacity: 0, transform: 'scale(0.85)' }),
      animate('380ms cubic-bezier(0.34, 1.56, 0.64, 1)',
        style({ opacity: 1, transform: 'scale(1)' }))
    ])
  ]),
];
