import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TaskLoaderComponent } from './task-loader.component';

describe('TaskLoaderComponent', () => {
  let component: TaskLoaderComponent;
  let fixture: ComponentFixture<TaskLoaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskLoaderComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskLoaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
