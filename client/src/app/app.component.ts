import { Component, OnInit } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

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
    private domSanitizer: DomSanitizer
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
}
