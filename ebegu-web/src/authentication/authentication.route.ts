import {RouterHelper} from '../dvbModules/router/route-helper-provider';
import {IState} from 'angular-ui-router';
import IStateParamsService = angular.ui.IStateParamsService;


authenticationRun.$inject = ['RouterHelper'];
/* @ngInject */
export function authenticationRun(routerHelper: RouterHelper) {
    routerHelper.configureStates(getStates(), '/login');
}

function getStates(): IState[] {
    return [
        new EbeguLoginState(),
        new EbeguLocalLoginState()
    ];
}

//STATES

export class EbeguLoginState implements IState {
    name = 'login';
    template = '<authentication-view>';
    //HINWEIS: Soweit ich sehen kann koennen url navigationen mit mehr als einem einzigen slash am Anfang nicht manuell in der Adressbar aufgerufen werden?
    url = '/login?type&relayPath';
}

export class EbeguLocalLoginState implements IState {
    name = 'locallogin';
    template = '<dummy-authentication-view>';
    url = '/locallogin';
}


export class IAuthenticationStateParams implements IStateParamsService {
    relayPath: string;
    type: string;
}

