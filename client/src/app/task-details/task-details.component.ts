import { OnInit, Component, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormControl } from '@angular/forms';
import { MatTableDataSource } from '@angular/material/table';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ReactiveFormsModule } from '@angular/forms';
import {MatPaginator, MatPaginatorModule} from '@angular/material/paginator';
import { CommonModule } from '@angular/common';
import { TaskTemplate } from '../interfaces/task-template';



@Component({
  selector: 'app-task-details',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatTableModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatPaginatorModule,
    CommonModule
  ],
  templateUrl: './task-details.component.html',
  styleUrl: './task-details.component.css'
})
export class TaskDetailsComponent implements OnInit {
  displayedColumns: string[] = ['day', 'data', 'actions'];
  dataSource = new MatTableDataSource<TaskTemplate>();
  isEditing: { [key: number]: boolean } = {};

  // Form for editing data
  editForms: { [key: number]: FormGroup } = {};

  

  @ViewChild(MatPaginator) paginator: MatPaginator = {} as MatPaginator;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    // Load data for the current month
    const today = new Date();
    const daysInMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate();
    
    const mockData = this.fetchDataForMonth(today.getMonth() + 1, today.getFullYear());
    
    const rows = Array.from({ length: daysInMonth }, (_, i) => {
      const day = i + 1;
      const dataForDay = mockData.find((entry) => entry.day === day) || { day, data: '' };
      this.editForms[day] = this.fb.group({
        data: new FormControl(dataForDay.data)
      });
      return dataForDay;
    });

    this.dataSource.data = rows;
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  fetchDataForMonth(month: number, year: number): any[] {
    // Simulated dynamic data fetch (replace with actual service call)
    return [
      { day: 1, data: 'Event 1' },
      { day: 15, data: 'Event 2' },
      { day: 20, data: 'Event 3' }
    ];
  }

  startEdit(day: number): void {
    this.isEditing[day] = true;
  }

  saveEdit(day: number): void {
    const updatedData = this.editForms[day].value.data;
    const rowIndex = this.dataSource.data.findIndex((row: any) => row.day === day);
    if (rowIndex !== -1) {
      this.dataSource.data[rowIndex].Task_Description = updatedData;
    }
    this.isEditing[day] = false;
  }

  cancelEdit(day: number): void {
    const rowIndex = this.dataSource.data.findIndex((row: any) => row.day === day);
    if (rowIndex !== -1) {
      this.editForms[day].setValue({ data: this.dataSource.data[rowIndex].Task_Description });
    }
    this.isEditing[day] = false;
  }
}
