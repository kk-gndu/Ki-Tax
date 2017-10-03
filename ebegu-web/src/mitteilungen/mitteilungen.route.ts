import {RouterHelper} from '../dvbModules/router/route-helper-provider';
import {IState, IStateParamsService} from 'angular-ui-router';

mitteilungenRun.$inject = ['RouterHelper'];

/* @ngInject */
export function mitteilungenRun(routerHelper: RouterHelper) {
    routerHelper.configureStates(getStates(), '/start');
}

function getStates(): IState[] {
    return [
        new EbeguMitteilungenStateState()
    ];
}

//STATES

export class EbeguMitteilungenStateState implements IState {
    name = 'mitteilungen';
    template = '<mitteilungen-view flex="auto" class="overflow-hidden" layout="column">';
    url = '/mitteilungen/:fallId/:betreuungId';
}

// PARAMS

export class IMitteilungenStateParams implements IStateParamsService {
    fallId: string;
    betreuungId: string;
    mitteilungId: string;
}
