import {NgModule} from '@angular/core';
import {RouterModule, Routes} from "@angular/router";

const routes: Routes = [
  {
    path: 'provision',
    loadChildren: () => import('../provision-page/provision-page.module').then(m => m.ProvisionPageModule)
  },
  {
    path: 'projects',
    loadChildren: () => import('../projects-page/projects-page.module').then(m => m.ProjectsPageModule)
  },
  {
    path: 'about',
    loadChildren: () => import('../about-page/about-page.module').then(m => m.AboutPageModule)
  },
  {
    path: '',
    redirectTo: '/projects',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/projects'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
