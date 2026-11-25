import { TestBed } from '@angular/core/testing';

import { ProjectService } from './project.service';
import { API_GENERATE_PROJECT_KEY_URL, API_PROJECT_DETAIL_URL, API_PROJECT_TEMPLATES_URL, API_PROJECT_URL } from '../../../tokens';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';

describe('ProjectService', () => {
  let service: ProjectService;
  let httpClientSpy;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        ProjectService,
        { provide: API_PROJECT_DETAIL_URL, useValue: '/project/FOO' },
        { provide: API_PROJECT_URL, useValue: '/project' },
        { provide: API_PROJECT_TEMPLATES_URL, useValue: '/not/needed/here' },
        { provide: API_GENERATE_PROJECT_KEY_URL, useValue: '/not/needed/here' }
      ]
    });
    httpMock = TestBed.inject(HttpTestingController);
    httpClientSpy = jasmine.createSpyObj('HttpClient', ['get']);
    service = TestBed.inject(ProjectService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all projects', async () => {
    const mockResponse = {
      FOO1: {
        projectName: 'FOO1'
      },
      FOO2: {},
      FOO3: {}
    };
    const mockResult = {
      FOO1: {
        projectName: 'FOO1'
      },
      FOO2: {},
      FOO3: {}
    };
    httpClientSpy.get.and.returnValue(of(mockResponse));
    service.getAllProjects().subscribe((projects: any) => {
      expect(projects.FOO1.projectName).toEqual(mockResult.FOO1.projectName);
    });
    httpMock.expectOne('/project').flush(mockResponse);
  });

  it('should log an error when getting all projects fails', async () => {
    const mockResponse = null;
    httpClientSpy.get.and.returnValue(of(mockResponse));
    service.getAllProjects().subscribe(
      () => fail(),
      (error: any) => {
        expect(error.status).toBe(404);
      }
    );
    httpMock.expectOne('/project').flush(null, {
      status: 404,
      statusText: ''
    });
  });

  it('should get a single project by its key', async () => {
    const mockResponse = { projectName: 'FOO' };
    const mockResult = { projectName: 'FOO' };
    httpClientSpy.get.and.returnValue(of(mockResponse));
    service.getProjectByKey('FOO').subscribe((project: any) => {
      expect(project).toEqual(mockResult);
    });
    httpMock.expectOne('/project/FOO').flush(mockResponse);
  });

  it('should log an error when getting a single project by its key fails', async () => {
    const mockResponse = null;
    httpClientSpy.get.and.returnValue(of(mockResponse));
    service.getProjectByKey('FOO').subscribe(
      () => fail(),
      (error: any) => {
        expect(error.status).toBe(404);
      }
    );
    httpMock.expectOne('/project/FOO', 'GET').flush(null, {
      status: 404,
      statusText: ''
    });
  });
});
