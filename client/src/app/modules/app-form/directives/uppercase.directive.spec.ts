import { Component, DebugElement } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { RemoveWhitespacesDirective } from './remove-whitespaces.directive';

xdescribe('UppercaseDirective', () => {
  @Component({
    template: `
      <form [formGroup]="formGroup">
        <input type="text" formControlName="formControl" uppercase />
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

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      declarations: [TestComponent, RemoveWhitespacesDirective]
    }).compileComponents();
  }));

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

  it('should transform value to uppercase while typing', () => {
    /* given */
    const control = component.formGroup.controls['formControl'];
    /* when */
    control.setValue('value');
    debugElement.triggerEventHandler('input', eventObject);
    /* then */
    expect(control.value).toEqual('VALUE');
  });
});
