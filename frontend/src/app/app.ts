import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MenuComponent } from './features/common/menu/menu';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MenuComponent, RouterOutlet],
 templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}