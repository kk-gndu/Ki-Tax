import TSAbstractEntity from './TSAbstractEntity';
import {TSTraegerschaft} from './TSTraegerschaft';
import {TSMandant} from './TSMandant';

export default class TSInstitution extends TSAbstractEntity {
    private _name: string;
    private _traegerschaft: TSTraegerschaft;
    private _mandant: TSMandant;
    private _mail: string;

    // just to communicate with client
    private _synchronizedWithOpenIdm: boolean = false;

    constructor(name?: string, tragerschaft?: TSTraegerschaft, mandant?: TSMandant, mail?: string) {
        super();
        this._name = name;
        this._traegerschaft = tragerschaft;
        this._mandant = mandant;
        this._mail = mail;
    }


    public get name(): string {
        return this._name;
    }

    public set name(value: string) {
        this._name = value;
    }

    public get traegerschaft(): TSTraegerschaft {
        return this._traegerschaft;
    }

    public set traegerschaft(value: TSTraegerschaft) {
        this._traegerschaft = value;
    }

    public get mandant(): TSMandant {
        return this._mandant;
    }

    public set mandant(value: TSMandant) {
        this._mandant = value;
    }

    get mail(): string {
        return this._mail;
    }

    set mail(value: string) {
        this._mail = value;
    }

    get synchronizedWithOpenIdm(): boolean {
        return this._synchronizedWithOpenIdm;
    }

    set synchronizedWithOpenIdm(value: boolean) {
        this._synchronizedWithOpenIdm = value;
    }
}
