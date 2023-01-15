import { Component, OnInit, Compiler, Injector, NgModuleFactory, Type } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { SessionStorageService } from 'ngx-webstorage';

import { VERSION } from 'app/app.constants';
import { LANGUAGES } from 'app/config/language.constants';
import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { EntityNavbarItems } from 'app/entities/entity-navbar-items';

@Component({
  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
})
export class NavbarComponent implements OnInit {
  inProduction?: boolean;
  isNavbarCollapsed = true;
  languages = LANGUAGES;
  openAPIEnabled?: boolean;
  version = '';
  account: Account | null = null;
  entitiesNavbarItems: any[] = [];
  metaenixEntityNavbarItems: any[] = [];
  metastoreEntityNavbarItems: any[] = [];
  metaliveEntityNavbarItems: any[] = [];

  constructor(
    private loginService: LoginService,
    private translateService: TranslateService,
    private sessionStorageService: SessionStorageService,
    private compiler: Compiler,
    private injector: Injector,
    private accountService: AccountService,
    private profileService: ProfileService,
    private router: Router
  ) {
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
  }

  ngOnInit(): void {
    this.entitiesNavbarItems = EntityNavbarItems;
    this.profileService.getProfileInfo().subscribe(profileInfo => {
      this.inProduction = profileInfo.inProduction;
      this.openAPIEnabled = profileInfo.openAPIEnabled;
    });

    this.accountService.getAuthenticationState().subscribe(account => {
      this.account = account;
      import('metaenix/entity-navbar-items').then(
        async ({ EntityNavbarItems: MetaenixEntityNavbarItems }) => {
          this.metaenixEntityNavbarItems = MetaenixEntityNavbarItems;
          const { LazyTranslationModule } = await import('metaenix/translation-module');
          this.loadModule(LazyTranslationModule as Type<any>);
        },
        error => {
          // eslint-disable-next-line no-console
          console.log('Error loading metaenix entities', error);
        }
      );
      import('metastore/entity-navbar-items').then(
        async ({ EntityNavbarItems: MetastoreEntityNavbarItems }) => {
          this.metastoreEntityNavbarItems = MetastoreEntityNavbarItems;
          const { LazyTranslationModule } = await import('metastore/translation-module');
          this.loadModule(LazyTranslationModule as Type<any>);
        },
        error => {
          // eslint-disable-next-line no-console
          console.log('Error loading metastore entities', error);
        }
      );
      import('metalive/entity-navbar-items').then(
        async ({ EntityNavbarItems: MetaliveEntityNavbarItems }) => {
          this.metaliveEntityNavbarItems = MetaliveEntityNavbarItems;
          const { LazyTranslationModule } = await import('metalive/translation-module');
          this.loadModule(LazyTranslationModule as Type<any>);
        },
        error => {
          // eslint-disable-next-line no-console
          console.log('Error loading metalive entities', error);
        }
      );
    });
  }

  changeLanguage(languageKey: string): void {
    this.sessionStorageService.store('locale', languageKey);
    this.translateService.use(languageKey);
  }

  collapseNavbar(): void {
    this.isNavbarCollapsed = true;
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  toggleNavbar(): void {
    this.isNavbarCollapsed = !this.isNavbarCollapsed;
  }

  private loadModule(moduleType: Type<any>): void {
    const moduleFactory = this.compiler.compileModuleAndAllComponentsSync(moduleType);
    moduleFactory.ngModuleFactory.create(this.injector);
  }
}
