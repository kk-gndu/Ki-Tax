import TSAbstractEntity from './TSAbstractEntity';
import {TSWizardStepName} from './enums/TSWizardStepName';
import {TSWizardStepStatus} from './enums/TSWizardStepStatus';

export default class TSWizardStep extends TSAbstractEntity {

    private _gesuchId: string;
    private _wizardStepName: TSWizardStepName;
    private _wizardStepStatus: TSWizardStepStatus;
    private _bemerkungen: string;
    private _verfuegbar: boolean;

    constructor(gesuchId?: string, wizardStepName?: TSWizardStepName, wizardStepStatus?: TSWizardStepStatus, bemerkungen?: string,
                verfuegbar?: boolean) {
        super();
        this._gesuchId = gesuchId;
        this._wizardStepName = wizardStepName;
        this._wizardStepStatus = wizardStepStatus;
        this._bemerkungen = bemerkungen;
        this._verfuegbar = verfuegbar;
    }

    get gesuchId(): string {
        return this._gesuchId;
    }

    set gesuchId(value: string) {
        this._gesuchId = value;
    }

    get wizardStepName(): TSWizardStepName {
        return this._wizardStepName;
    }

    set wizardStepName(value: TSWizardStepName) {
        this._wizardStepName = value;
    }

    get wizardStepStatus(): TSWizardStepStatus {
        return this._wizardStepStatus;
    }

    set wizardStepStatus(value: TSWizardStepStatus) {
        this._wizardStepStatus = value;
    }

    get bemerkungen(): string {
        return this._bemerkungen;
    }

    set bemerkungen(value: string) {
        this._bemerkungen = value;
    }

    get verfuegbar(): boolean {
        return this._verfuegbar;
    }

    set verfuegbar(value: boolean) {
        this._verfuegbar = value;
    }
}
