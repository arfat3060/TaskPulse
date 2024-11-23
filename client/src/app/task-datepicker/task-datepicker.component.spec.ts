import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskDatepickerComponent } from './task-datepicker.component';

describe('TaskDatepickerComponent', () => {
  let component: TaskDatepickerComponent;
  let fixture: ComponentFixture<TaskDatepickerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskDatepickerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskDatepickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
