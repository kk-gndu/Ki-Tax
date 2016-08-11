import {IComponentOptions, IFormController} from 'angular';
import AbstractGesuchViewController from '../abstractGesuchView';
import GesuchModelManager from '../../service/gesuchModelManager';
import {IStateService} from 'angular-ui-router';
import {IStammdatenStateParams} from '../../gesuch.route';
import TSFinanzielleSituationContainer from '../../../models/TSFinanzielleSituationContainer';
import BerechnungsManager from '../../service/berechnungsManager';
import TSFinanzielleSituationResultateDTO from '../../../models/dto/TSFinanzielleSituationResultateDTO';
import ErrorService from '../../../core/errors/service/ErrorService';
let template = require('./finanzielleSituationView.html');
require('./finanzielleSituationView.less');


export class FinanzielleSituationViewComponentConfig implements IComponentOptions {
    transclude = false;
    template = template;
    controller = FinanzielleSituationViewController;
    controllerAs = 'vm';
}

export class FinanzielleSituationViewController extends AbstractGesuchViewController {

    public showSelbstaendig: boolean;
    static $inject: string[] = ['$stateParams', '$state', 'GesuchModelManager', 'BerechnungsManager', 'CONSTANTS', 'ErrorService'];
    /* @ngInject */
    constructor($stateParams: IStammdatenStateParams, $state: IStateService, gesuchModelManager: GesuchModelManager,
                berechnungsManager: BerechnungsManager, private CONSTANTS: any, private errorService: ErrorService) {
        super($state, gesuchModelManager, berechnungsManager);
        let parsedNum: number = parseInt($stateParams.gesuchstellerNumber, 10);
        this.gesuchModelManager.setGesuchstellerNumber(parsedNum);
        this.initViewModel();
        this.calculate();
    }

    private initViewModel() {
        this.gesuchModelManager.initFinanzielleSituation();
        this.showSelbstaendig = this.gesuchModelManager.getStammdatenToWorkWith().finanzielleSituationContainer.finanzielleSituationJA.isSelbstaendig();
    }

    public showSelbstaendigClicked() {
        if (!this.showSelbstaendig) {
            this.resetSelbstaendigFields();
        }
    }

    private resetSelbstaendigFields() {
        if (this.gesuchModelManager.getStammdatenToWorkWith() && this.gesuchModelManager.getStammdatenToWorkWith().finanzielleSituationContainer) {
            this.gesuchModelManager.getStammdatenToWorkWith().finanzielleSituationContainer.finanzielleSituationJA.geschaeftsgewinnBasisjahr = undefined;
            this.gesuchModelManager.getStammdatenToWorkWith().finanzielleSituationContainer.finanzielleSituationJA.geschaeftsgewinnBasisjahrMinus1 = undefined;
            this.gesuchModelManager.getStammdatenToWorkWith().finanzielleSituationContainer.finanzielleSituationJA.geschaeftsgewinnBasisjahrMinus2 = undefined;
            this.calculate();
        }
    }

    showSteuerveranlagung(): boolean {
        return !this.gesuchModelManager.getFamiliensituation().gemeinsameSteuererklaerung || this.gesuchModelManager.getFamiliensituation().gemeinsameSteuererklaerung === false;
    }

    showSteuererklaerung(): boolean {
        return this.gesuchModelManager.getStammdatenToWorkWith().finanzielleSituationContainer.finanzielleSituationJA.steuerveranlagungErhalten === false;
    }

    private steuerveranlagungClicked(): void {
        // Wenn Steuerveranlagung JA -> auch StekErhalten -> JA
        if (this.getModel().finanzielleSituationJA.steuerveranlagungErhalten === true) {
            this.getModel().finanzielleSituationJA.steuererklaerungAusgefuellt = true;
        } else if (this.getModel().finanzielleSituationJA.steuerveranlagungErhalten === false) {
            // Steuerveranlagung neu NEIN -> Fragen loeschen
            this.getModel().finanzielleSituationJA.steuererklaerungAusgefuellt = undefined;
        }
    }

    previousStep() {
        if ((this.gesuchModelManager.getGesuchstellerNumber() === 2)) {
            this.state.go('gesuch.finanzielleSituation', {gesuchstellerNumber: 1});
        } else if ((this.gesuchModelManager.gesuchstellerNumber === 1) && this.gesuchModelManager.isGesuchsteller2Required()) {
            this.state.go('gesuch.finanzielleSituationStart');
        } else {
            this.state.go('gesuch.kinder');
        }
    }

    nextStep() {
        if ((this.gesuchModelManager.getGesuchstellerNumber() === 1) && this.gesuchModelManager.isGesuchsteller2Required()) {
            this.state.go('gesuch.finanzielleSituation', {gesuchstellerNumber: '2'});
        } else {
            this.state.go('gesuch.finanzielleSituationResultate');
        }
    }

    submit(form: IFormController) {
        if (form.$valid) {
            // Speichern ausloesen
            this.errorService.clearAll();
            this.gesuchModelManager.saveFinanzielleSituation().then((finanzielleSituationResponse: any) => {
                this.nextStep();
            });
        }
    }

    calculate() {
        this.berechnungsManager.calculateFinanzielleSituation(this.gesuchModelManager.gesuch);
    }

    resetForm() {
        this.initViewModel();
    }

    public getModel(): TSFinanzielleSituationContainer {
        return this.gesuchModelManager.getStammdatenToWorkWith().finanzielleSituationContainer;
    }

    public getResultate(): TSFinanzielleSituationResultateDTO {
        return this.berechnungsManager.finanzielleSituationResultate;
    }

    /**
     * Mindestens einer aller Felder von Geschaftsgewinn muss ausgefuellt sein. Mit dieser Methode kann man es pruefen.
     * @returns {boolean}
     */
    public isGeschaeftsgewinnRequired(): boolean {
        return !(this.getModel().finanzielleSituationJA.geschaeftsgewinnBasisjahr ||
        this.getModel().finanzielleSituationJA.geschaeftsgewinnBasisjahrMinus1 ||
        this.getModel().finanzielleSituationJA.geschaeftsgewinnBasisjahrMinus2);
    }
}
