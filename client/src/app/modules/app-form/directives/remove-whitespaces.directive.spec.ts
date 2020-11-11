import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { RemoveWhitespacesDirective } from './remove-whitespaces.directive';

/* TODO refactor using spectator */
describe('RemoveWhitespacesDirective', () => {
  @Component({
    template: `
      <form [formGroup]="formGroup">
        <input type="text" formControlName="formControl" removeWhitespaces />
      </form>
    `
  })
  class TestComponent {
    formGroup: FormGroup = new FormGroup({
      formControl: new FormControl('')
    });
  }

  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let debugElement: DebugElement;
  let nativeElement: HTMLInputElement;
  let eventObject: any;

  beforeEach(
    waitForAsync(() => {
      TestBed.configureTestingModule({
        imports: [ReactiveFormsModule],
        declarations: [TestComponent, RemoveWhitespacesDirective]
      }).compileComponents();
    })
  );

  beforeEach(() => {
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    debugElement = fixture.debugElement.query(
      By.directive(RemoveWhitespacesDirective)
    );
    nativeElement = debugElement.nativeElement;
    eventObject = {
      target: nativeElement,
      preventDefault: () => {}
    };
  });

  it('should remove whitespaces', () => {
    /* given */
    const control = component.formGroup.controls.formControl;
    /* when */
    control.setValue('  v  a  l  u  e  ');
    debugElement.triggerEventHandler('blur', eventObject);
    /* then */
    expect(control.value).toEqual('value');
  });

  it('should not remove any character', () => {
    /* given */
    const control = component.formGroup.controls.formControl;
    /* when */
    control.setValue('values');
    debugElement.triggerEventHandler('blur', eventObject);
    /* then */
    expect(control.value).toEqual('values');
  });
});
