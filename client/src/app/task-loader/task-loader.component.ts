import { Component } from '@angular/core';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';

@Component({
  selector: 'app-task-loader',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  templateUrl: './task-loader.component.html',
  styleUrl: './task-loader.component.css'
})
export class TaskLoaderComponent {

}
