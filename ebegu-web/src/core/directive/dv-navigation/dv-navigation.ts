import {IDirective, IDirectiveFactory} from 'angular';
import {IStateService} from 'angular-ui-router';
import WizardStepManager from '../../../gesuch/service/wizardStepManager';
import {TSWizardStepName} from '../../../models/enums/TSWizardStepName';
import GesuchModelManager from '../../../gesuch/service/gesuchModelManager';
import AuthServiceRS from '../../../authentication/service/AuthServiceRS.rest';
import {TSRole} from '../../../models/enums/TSRole';
import ErrorService from '../../errors/service/ErrorService';
import {TSWizardStepStatus} from '../../../models/enums/TSWizardStepStatus';
import ITranslateService = angular.translate.ITranslateService;
let template = require('./dv-navigation.html');

/**
 * Diese Direktive wird benutzt, um die Navigation Buttons darzustellen. Folgende Parameter koennen benutzt werden,
 * um die Funktionalitaet zu definieren:
 *
 * -- dvPrevious: function      Wenn true wird der Button "previous" angezeigt (nicht gleichzeitig mit dvCancel benutzen)
 * -- dvNext: function          Wenn true wird der Button "next" angezeigt
 * -- dvNextDisabled: function  Wenn man eine extra Pruefung braucht, um den Button Next zu disablen
 * -- dvSubStep: number         Manche Steps haben sog. SubSteps (z.B. finanzielle Situation). Dieses Parameter wird benutzt,
 *                              um zwischen SubSteps unterscheiden zu koennen
 * -- dvSave: function          Die callback Methode, die man aufrufen muss, wenn der Button geklickt wird. Verwenden nur um die Daten zu speichern
 * -- dvCancel: function        Die callback Methode, um alles zurueckzusetzen (nicht gleichzeitig mit dvPrevious benutzen)
 */
export class DVNavigation implements IDirective {
    restrict = 'E';
    scope = {
        dvPrevious: '&?',
        dvNext: '&?',
        dvCancel: '&?',
        dvNextDisabled: '&?',
        dvSubStep: '<',
        dvSave: '&?'
    };
    controller = NavigatorController;
    controllerAs = 'vm';
    bindToController = true;
    template = template;

    static factory(): IDirectiveFactory {
        const directive = () => new DVNavigation();
        directive.$inject = [];
        return directive;
    }
}
/**
 * Direktive  der initial die smart table nach dem aktuell eingeloggtem user filtert
 */
export class NavigatorController {

    dvPrevious: () => any;
    dvNext: () => any;
    dvSave: () => any;
    dvCancel: () => any;
    dvNextDisabled: () => any;
    dvSubStep: number;

    static $inject: string[] = ['WizardStepManager', '$state', 'GesuchModelManager', 'AuthServiceRS', '$translate', 'ErrorService'];
    /* @ngInject */
    constructor(private wizardStepManager: WizardStepManager, private state: IStateService, private gesuchModelManager: GesuchModelManager,
                private authServiceRS: AuthServiceRS, private $translate: ITranslateService, private errorService: ErrorService) {
    }

    public doesCancelExist(): boolean {
        return this.dvCancel !== undefined && this.dvCancel !== null;
    }

    /**
     * Wenn die function save uebergeben wurde, dann heisst der Button Speichern und weiter. Sonst nur weiter
     * @returns {string}
     */
    public getPreviousButtonName(): string {
        if (this.gesuchModelManager.isGesuchStatusVerfuegenVerfuegt()) {
            return this.$translate.instant('ZURUECK_ONLY_UPPER');
        } else if (this.dvSave) {
            return this.$translate.instant('ZURUECK_UPPER');
        } else {
            return this.$translate.instant('ZURUECK_ONLY_UPPER');
        }
    }

    /**
     * Wenn die function save uebergeben wurde, dann heisst der Button Speichern und zurueck. Sonst nur zurueck
     * @returns {string}
     */
    public getNextButtonName(): string {
        if (this.gesuchModelManager.isGesuchStatusVerfuegenVerfuegt()) {
            return this.$translate.instant('WEITER_ONLY_UPPER');
        } else if (this.dvSave) {
            return this.$translate.instant('WEITER_UPPER');
        } else {
            return this.$translate.instant('WEITER_ONLY_UPPER');
        }
    }

    /**
     * Diese Methode prueft zuerst ob eine Function in dvSave uebergeben wurde. In diesem Fall wird diese Methode aufgerufen und erst
     * als callback zum naechsten Step gefuehrt. Wenn dvSave keine gueltige Function enthaelt und deshalb keine Promise zurueckgibt,
     * wird dann direkt zum naechsten Step geleitet.
     */
    public nextStep(): void {
        if (!this.gesuchModelManager.isGesuchStatusVerfuegenVerfuegt() && this.dvSave) {
            this.dvSave().then(() => {
                this.navigateToNextStep();
            });
        } else {
            this.navigateToNextStep();
        }
    }

    /**
     * Diese Methode prueft zuerst ob eine Function in dvSave uebergeben wurde. In diesem Fall wird diese Methode aufgerufen und erst
     * als callback zum vorherigen Step gefuehrt. Wenn dvSave keine gueltige Function enthaelt und deshalb keine Promise zurueckgibt,
     * wird dann direkt zum vorherigen Step geleitet.
     */
    public previousStep(): void {
        if (!this.gesuchModelManager.isGesuchStatusVerfuegenVerfuegt() && this.dvSave) {
            this.dvSave().then(() => {
                this.navigateToPreviousStep();
            });
        } else {
            this.navigateToPreviousStep();
        }
    }

    /**
     * Diese Methode ist aehnlich wie previousStep() aber wird verwendet, um die Aenderungen NICHT zu speichern
     */
    public cancel(): void {
        if (this.dvCancel) {
            this.dvCancel();
        }
        this.navigateToPreviousStep();
    }

    /**
     * Berechnet fuer den aktuellen Benutzer und Step, welcher der naechste Step ist und wechselt zu diesem.
     */
    private navigateToNextStep() {
        let gesuchId = this.getGesuchId();

        this.errorService.clearAll();
        if (TSWizardStepName.GESUCH_ERSTELLEN === this.wizardStepManager.getCurrentStepName()) {
            this.state.go('gesuch.familiensituation', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.FAMILIENSITUATION === this.wizardStepManager.getCurrentStepName()) {
            this.state.go('gesuch.stammdaten', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.GESUCHSTELLER === this.wizardStepManager.getCurrentStepName()) {
            if ((this.gesuchModelManager.getGesuchstellerNumber() === 1) && this.gesuchModelManager.isGesuchsteller2Required()) {
                this.state.go('gesuch.stammdaten', {
                    gesuchstellerNumber: '2',
                    gesuchId: gesuchId
                });
            } else {
                if (this.authServiceRS.isRole(TSRole.SACHBEARBEITER_INSTITUTION)
                    || this.authServiceRS.isRole(TSRole.SACHBEARBEITER_TRAEGERSCHAFT)) {
                    this.state.go('gesuch.betreuungen', {
                        gesuchId: gesuchId
                    });
                } else {
                    this.state.go('gesuch.kinder', {
                        gesuchId: gesuchId
                    });
                }
            }
        } else if (TSWizardStepName.KINDER === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            this.state.go('gesuch.betreuungen', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.KINDER === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.state.go('gesuch.kinder', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.BETREUUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            if (this.authServiceRS.isRole(TSRole.SACHBEARBEITER_INSTITUTION)
                || this.authServiceRS.isRole(TSRole.SACHBEARBEITER_TRAEGERSCHAFT)) {
                this.state.go('gesuch.verfuegen', {
                    gesuchId: gesuchId
                });
            } else {
                this.state.go('gesuch.erwerbsPensen', {
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.BETREUUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            // Diese Logik ist ziemlich kompliziert. Deswegen bleibt sie noch in betreuungView.ts -> Hier wird dann nichts gemacht

        } else if (TSWizardStepName.ERWERBSPENSUM === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            this.errorService.clearAll();
            if (this.wizardStepManager.isNextStepBesucht() && !this.wizardStepManager.isNextStepEnabled()) {
                // wenn finanzielle situation besucht aber nicht enabled ist, dann zu Dokumenten
                this.state.go('gesuch.dokumente', {
                    gesuchId: gesuchId
                });
            } else if (this.gesuchModelManager.isGesuchsteller2Required()) {
                this.state.go('gesuch.finanzielleSituationStart', {
                    gesuchId: gesuchId
                });
            } else {
                this.state.go('gesuch.finanzielleSituation', {
                    gesuchstellerNumber: 1,
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.ERWERBSPENSUM === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.state.go('gesuch.erwerbsPensen', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.FINANZIELLE_SITUATION === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            if ((this.gesuchModelManager.getGesuchstellerNumber() === 1) && this.gesuchModelManager.isGesuchsteller2Required()) {
                this.state.go('gesuch.finanzielleSituation', {
                    gesuchstellerNumber: '2',
                    gesuchId: gesuchId
                });
            } else {
                this.state.go('gesuch.finanzielleSituationResultate', {
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.FINANZIELLE_SITUATION === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.state.go('gesuch.finanzielleSituation', {
                gesuchstellerNumber: '1',
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.FINANZIELLE_SITUATION === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 3) {
            this.state.go('gesuch.einkommensverschlechterungInfo', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.EINKOMMENSVERSCHLECHTERUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            if (this.gesuchModelManager.getEinkommensverschlechterungsInfo().einkommensverschlechterung) { // was muss hier sein?
                if (this.gesuchModelManager.isGesuchsteller2Required()) {
                    this.state.go('gesuch.einkommensverschlechterungSteuern', {
                        gesuchId: gesuchId
                    });
                } else {
                    this.state.go('gesuch.einkommensverschlechterung', {
                        gesuchstellerNumber: '1',
                        gesuchId: gesuchId
                    });
                }
            } else {
                this.state.go('gesuch.dokumente', {
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.EINKOMMENSVERSCHLECHTERUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.navigateNextEVSubStep2(gesuchId);

        } else if (TSWizardStepName.EINKOMMENSVERSCHLECHTERUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 3) {
            this.state.go('gesuch.einkommensverschlechterung', {
                gesuchstellerNumber: '1', basisjahrPlus: '1',
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.EINKOMMENSVERSCHLECHTERUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 4) {
            this.navigateNextEVSubStep4(gesuchId);

        } else if (TSWizardStepName.DOKUMENTE === this.wizardStepManager.getCurrentStepName()) {
            this.state.go('gesuch.verfuegen', {
                gesuchId: gesuchId
            });
        }
    }

    /**
     * Berechnet fuer den aktuellen Benutzer und Step, welcher der previous Step ist und wechselt zu diesem.
     */
    private navigateToPreviousStep() {
        let gesuchId = this.getGesuchId();

        this.errorService.clearAll();
        if (TSWizardStepName.FAMILIENSITUATION === this.wizardStepManager.getCurrentStepName()) {
            this.state.go('gesuch.fallcreation', {
                createNew: 'false',
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.GESUCHSTELLER === this.wizardStepManager.getCurrentStepName()) {
            if ((this.gesuchModelManager.getGesuchstellerNumber() === 2)) {
                this.state.go('gesuch.stammdaten', {
                    gesuchstellerNumber: '1',
                    gesuchId: gesuchId
                });
            } else {
                this.state.go('gesuch.familiensituation', {
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.KINDER === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            this.moveBackToGesuchsteller(gesuchId);

        } else if (TSWizardStepName.KINDER === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.state.go('gesuch.kinder', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.BETREUUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            if (this.authServiceRS.isRole(TSRole.SACHBEARBEITER_INSTITUTION)
                || this.authServiceRS.isRole(TSRole.SACHBEARBEITER_TRAEGERSCHAFT)) {
                this.moveBackToGesuchsteller(gesuchId);
            } else {
                this.state.go('gesuch.kinder', {
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.BETREUUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.state.go('gesuch.betreuungen', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.ERWERBSPENSUM === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            this.state.go('gesuch.betreuungen', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.ERWERBSPENSUM === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.state.go('gesuch.erwerbsPensen', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.FINANZIELLE_SITUATION === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            if ((this.gesuchModelManager.getGesuchstellerNumber() === 2)) {
                this.state.go('gesuch.finanzielleSituation', {
                    gesuchstellerNumber: 1,
                    gesuchId: gesuchId
                });
            } else if (this.gesuchModelManager.getGesuchstellerNumber() === 1 && this.gesuchModelManager.isGesuchsteller2Required()) {
                this.state.go('gesuch.finanzielleSituationStart', {
                    gesuchId: gesuchId
                });
            } else {
                this.state.go('gesuch.kinder', {
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.FINANZIELLE_SITUATION === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.state.go('gesuch.erwerbsPensen', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.FINANZIELLE_SITUATION === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 3) {
            if ((this.gesuchModelManager.getGesuchstellerNumber() === 2)) {
                this.state.go('gesuch.finanzielleSituation', {
                    gesuchstellerNumber: 2,
                    gesuchId: gesuchId
                });
            } else {
                this.state.go('gesuch.finanzielleSituation', {
                    gesuchstellerNumber: 1,
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.EINKOMMENSVERSCHLECHTERUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            this.state.go('gesuch.finanzielleSituation', {
                gesuchstellerNumber: '1',
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.EINKOMMENSVERSCHLECHTERUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.navigatePreviousEVSubStep2(gesuchId);

        } else if (TSWizardStepName.EINKOMMENSVERSCHLECHTERUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 3) {
            this.state.go('gesuch.einkommensverschlechterungInfo', {
                gesuchId: gesuchId
            });

        } else if (TSWizardStepName.EINKOMMENSVERSCHLECHTERUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 4) {
            this.navigatePreviousEVSubStep4(gesuchId);

        } else if (TSWizardStepName.DOKUMENTE === this.wizardStepManager.getCurrentStepName()) {
            this.navigatePreviousDokumente(gesuchId);

        } else if (TSWizardStepName.VERFUEGEN === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            if (this.authServiceRS.isRole(TSRole.SACHBEARBEITER_INSTITUTION)
                || this.authServiceRS.isRole(TSRole.SACHBEARBEITER_TRAEGERSCHAFT)) {
                this.state.go('gesuch.betreuungen', {
                    gesuchId: gesuchId
                });
            } else {
                this.state.go('gesuch.dokumente', {
                    gesuchId: gesuchId
                });
            }

        } else if (TSWizardStepName.VERFUEGEN === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 2) {
            this.state.go('gesuch.verfuegen', {
                gesuchId: gesuchId
            });
        }
    }

    private getGesuchId(): string {
        if (this.gesuchModelManager && this.gesuchModelManager.getGesuch()) {
            return this.gesuchModelManager.getGesuch().id;
        }
        return '';
    }

    /**
     * Checks whether the button should be disable for the current conditions. By default enabled
     * @returns {boolean}
     */
    public isNextDisabled(): boolean {
        if (TSWizardStepName.KINDER === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            return !this.gesuchModelManager.isThereAnyKindWithBetreuungsbedarf() && !this.wizardStepManager.isNextStepBesucht();
        }
        if (TSWizardStepName.BETREUUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            return !this.gesuchModelManager.isThereAnyBetreuung() && !this.wizardStepManager.isNextStepBesucht();
        }
        if (TSWizardStepName.ERWERBSPENSUM === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
            return this.dvNextDisabled() && !this.wizardStepManager.isNextStepBesucht();
        }
        return false;
    }

    public getTooltip(): string {
        if (this.isNextDisabled()) {
            if (TSWizardStepName.KINDER === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
                return this.$translate.instant('KINDER_TOOLTIP_REQUIRED');

            } else if (TSWizardStepName.BETREUUNG === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
                return this.$translate.instant('BETREUUNG_TOOLTIP_REQUIRED');

            } else if (TSWizardStepName.ERWERBSPENSUM === this.wizardStepManager.getCurrentStepName() && this.dvSubStep === 1) {
                return this.$translate.instant('ERWERBSPENSUM_TOOLTIP_REQUIRED');
            }
        }
        return undefined;
    }

    private navigateNextEVSubStep2(gesuchId: string): void {
        if ((this.gesuchModelManager.getBasisJahrPlusNumber() === 1)) {
            if (this.gesuchModelManager.getGesuchstellerNumber() === 1) {
                // 1 , 1
                if (this.gesuchModelManager.isBasisJahr2Required()) {
                    this.state.go('gesuch.einkommensverschlechterung', {
                        gesuchstellerNumber: '1',
                        basisjahrPlus: '2',
                        gesuchId: gesuchId
                    });
                } else if (this.gesuchModelManager.isGesuchsteller2Required()) {
                    this.state.go('gesuch.einkommensverschlechterung', {
                        gesuchstellerNumber: '2',
                        basisjahrPlus: '1',
                        gesuchId: gesuchId
                    });
                } else {
                    this.state.go('gesuch.einkommensverschlechterungResultate', {
                        basisjahrPlus: '1',
                        gesuchId: gesuchId
                    });
                }

            } else { //gesuchsteller ===2
                // 2 , 1
                if (this.gesuchModelManager.isBasisJahr2Required()) {
                    this.state.go('gesuch.einkommensverschlechterung', {
                        gesuchstellerNumber: '2',
                        basisjahrPlus: '2',
                        gesuchId: gesuchId
                    });
                } else {
                    this.state.go('gesuch.einkommensverschlechterungResultate', {
                        basisjahrPlus: '1',
                        gesuchId: gesuchId
                    });
                }

            }

        } else if ((this.gesuchModelManager.getBasisJahrPlusNumber() === 2)) {
            if (this.gesuchModelManager.getGesuchstellerNumber() === 1) {
                // 1 , 2
                if (this.gesuchModelManager.isGesuchsteller2Required()) {
                    this.state.go('gesuch.einkommensverschlechterung', {
                        gesuchstellerNumber: '2',
                        basisjahrPlus: '1',
                        gesuchId: gesuchId
                    });
                } else {
                    this.state.go('gesuch.einkommensverschlechterungResultate', {
                        basisjahrPlus: '1',
                        gesuchId: gesuchId
                    });
                }

            } else { //gesuchsteller ===2
                // 2 , 2
                this.state.go('gesuch.einkommensverschlechterungResultate', {
                    basisjahrPlus: '1',
                    gesuchId: gesuchId
                });
            }
        }
    };

    private navigatePreviousEVSubStep2(gesuchId: string): void {
        if ((this.gesuchModelManager.getBasisJahrPlusNumber() === 1)) {
            if (this.gesuchModelManager.getGesuchstellerNumber() === 1) {
                // 1 , 1
                if (this.gesuchModelManager.isGesuchsteller2Required()) {
                    this.state.go('gesuch.einkommensverschlechterungSteuern', {
                        gesuchId: gesuchId
                    });
                } else {
                    this.state.go('gesuch.einkommensverschlechterungInfo', {
                        gesuchId: gesuchId
                    });
                }
            } else { //gesuchsteller ===2
                // 2 , 1
                if (this.gesuchModelManager.isBasisJahr2Required()) {
                    this.state.go('gesuch.einkommensverschlechterung', {
                        gesuchstellerNumber: '1',
                        basisjahrPlus: '2',
                        gesuchId: gesuchId
                    });
                } else {
                    this.state.go('gesuch.einkommensverschlechterung', {
                        gesuchstellerNumber: '1',
                        basisjahrPlus: '1',
                        gesuchId: gesuchId
                    });
                }
            }
        } else if ((this.gesuchModelManager.getBasisJahrPlusNumber() === 2)) {
            if (this.gesuchModelManager.getGesuchstellerNumber() === 1) {
                // 1 , 2
                this.state.go('gesuch.einkommensverschlechterung', {
                    gesuchstellerNumber: '1',
                    basisjahrPlus: '1',
                    gesuchId: gesuchId
                });
            } else { //gesuchsteller ===2
                // 2 , 2
                this.state.go('gesuch.einkommensverschlechterung', {
                    gesuchstellerNumber: '2',
                    basisjahrPlus: '1',
                    gesuchId: gesuchId
                });
            }
        }
    };

    private navigatePreviousEVSubStep4(gesuchId: string): void {
        if (this.gesuchModelManager.getBasisJahrPlusNumber() === 2) {
            this.state.go('gesuch.einkommensverschlechterungResultate', {
                basisjahrPlus: '1',
                gesuchId: gesuchId
            });
        } else {
            // baisjahrPlus1

            let gesuchsteller2Required: boolean = this.gesuchModelManager.isGesuchsteller2Required();
            let basisJahr2Required: boolean = this.gesuchModelManager.isBasisJahr2Required();

            if (gesuchsteller2Required && basisJahr2Required) {
                this.state.go('gesuch.einkommensverschlechterung', {
                    gesuchstellerNumber: '2',
                    basisjahrPlus: '2',
                    gesuchId: gesuchId
                });
            } else if (gesuchsteller2Required) {
                this.state.go('gesuch.einkommensverschlechterung', {
                    gesuchstellerNumber: '2',
                    basisjahrPlus: '1',
                    gesuchId: gesuchId
                });
            } else if (basisJahr2Required) {
                this.state.go('gesuch.einkommensverschlechterung', {
                    gesuchstellerNumber: '1',
                    basisjahrPlus: '2',
                    gesuchId: gesuchId
                });
            } else {
                this.state.go('gesuch.einkommensverschlechterung', {
                    gesuchstellerNumber: '1',
                    basisjahrPlus: '1',
                    gesuchId: gesuchId
                });
            }
        }
    };

    //muss als instance arrow function definiert werden statt als prototyp funktionw eil sonst this undefined ist
    private navigateNextEVSubStep4(gesuchId: string): void {
        if (this.gesuchModelManager.getBasisJahrPlusNumber() === 2) {
            this.goToDokumenteView(gesuchId);
        } else {
            let ekvFuerBasisJahrPlus2 = this.gesuchModelManager.getGesuch().einkommensverschlechterungInfo.ekvFuerBasisJahrPlus2
                && this.gesuchModelManager.getGesuch().einkommensverschlechterungInfo.ekvFuerBasisJahrPlus2 === true;
            if (ekvFuerBasisJahrPlus2) {
                this.state.go('gesuch.einkommensverschlechterungResultate', {
                    basisjahrPlus: '2',
                    gesuchId: gesuchId
                });
            } else {
                this.goToDokumenteView(gesuchId);
            }
        }
    };

    /**
     * Goes to the view of documents and updates before the status of the WizardStep Einkommensverschlechterung to OK
     */
    private goToDokumenteView(gesuchId: string) {
        this.wizardStepManager.updateCurrentWizardStepStatus(TSWizardStepStatus.OK).then(() => {
            this.state.go('gesuch.dokumente', {
                gesuchId: gesuchId
            });
        });
    }

    private navigatePreviousDokumente(gesuchId: string): void {
        let ekvFuerBasisJahrPlus2 = this.gesuchModelManager.getGesuch().einkommensverschlechterungInfo.ekvFuerBasisJahrPlus2
            && this.gesuchModelManager.getGesuch().einkommensverschlechterungInfo.ekvFuerBasisJahrPlus2 === true;
        let ekvFuerBasisJahrPlus1 = this.gesuchModelManager.getGesuch().einkommensverschlechterungInfo.ekvFuerBasisJahrPlus1
            && this.gesuchModelManager.getGesuch().einkommensverschlechterungInfo.ekvFuerBasisJahrPlus1 === true;
        if (ekvFuerBasisJahrPlus2) {
            this.state.go('gesuch.einkommensverschlechterungResultate', {
                basisjahrPlus: '2',
                gesuchId: gesuchId
            });
        } else if (ekvFuerBasisJahrPlus1) {
            this.state.go('gesuch.einkommensverschlechterungResultate', {
                basisjahrPlus: '1',
                gesuchId: gesuchId
            });
        } else {
            this.state.go('gesuch.einkommensverschlechterungInfo', {
                gesuchId: gesuchId
            });
        }
    }

    private moveBackToGesuchsteller(gesuchId: string) {
        if ((this.gesuchModelManager.getGesuchstellerNumber() === 2)) {
            this.state.go('gesuch.stammdaten', {
                gesuchstellerNumber: 2,
                gesuchId: gesuchId
            });
        } else {
            this.state.go('gesuch.stammdaten', {
                gesuchstellerNumber: 1,
                gesuchId: gesuchId
            });
        }
    }

}
