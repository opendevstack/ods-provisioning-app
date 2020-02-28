import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FormBaseComponent } from './form-base.component';
import { Component } from '@angular/core';

describe('FormBaseComponent', () => {
  @Component({
    selector: 'form-base',
    template: ''
  })
  class TestComponent extends FormBaseComponent {
    constructor() {
      super();
    }
  }

  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
