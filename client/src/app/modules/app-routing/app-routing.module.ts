import {NgModule} from '@angular/core';
import {RouterModule, Routes} from "@angular/router";

const routes: Routes = [
  {
    path: 'provision',
    loadChildren: () => import('../provision-page/provision-page.module').then(m => m.ProvisionPageModule)
  },
  {
    path: 'project',
    loadChildren: () => import('../project-page/project-page.module').then(m => m.ProjectPageModule)
  },
  {
    path: 'about',
    loadChildren: () => import('../about-page/about-page.module').then(m => m.AboutPageModule)
  },
  {
    path: '',
    redirectTo: '/project',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/project'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
