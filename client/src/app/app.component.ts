import { Component, OnInit, Renderer2 } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { EditModeService } from './modules/edit-mode/services/edit-mode.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  title = 'Prov-App';

  isLoading = false;

  projects: any = [
    {
      name: 'Airflow Testing Grounds',
      key: 'AIRF'
    },
    {
      name:
        'ASAP - Augmented Synthesis and Analytical data-based Process development',
      key: 'ASAP'
    },
    {
      name: 'Benefit Risk Analytic SyStem (BRASS)',
      key: 'BRASS'
    },
    {
      name: 'LIFE | BI X Design System',
      key: 'LIFE'
    }
  ];

  constructor(
    private matIconRegistry: MatIconRegistry,
    private domSanitizer: DomSanitizer,
    private renderer: Renderer2,
    private editModeService: EditModeService
  ) {
    this.matIconRegistry.addSvgIconSet(
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        '../assets/icons/mdi-custom-icons.svg'
      )
    );
    this.matIconRegistry.addSvgIconSet(
      this.domSanitizer.bypassSecurityTrustResourceUrl(
        '../assets/icons/bi-stack.svg'
      )
    );
  }

  ngOnInit(): void {}

  getEditModeStatus(instance: any): void {
    this.editModeService.onGetEditModeFlag.subscribe(editModeActive => {
      if (editModeActive) {
        this.renderer.addClass(document.body, 'status-editmode-active');
      } else {
        this.renderer.removeClass(document.body, 'status-editmode-active');
      }
    });
  }
}
