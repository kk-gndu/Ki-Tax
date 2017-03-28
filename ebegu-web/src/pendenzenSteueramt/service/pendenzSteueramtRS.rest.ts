import EbeguRestUtil from '../../utils/EbeguRestUtil';
import {IHttpService, IPromise, ILogService} from 'angular';
import TSAntragDTO from '../../models/TSAntragDTO';

export default class PendenzSteueramtRS {
    serviceURL: string;
    http: IHttpService;
    ebeguRestUtil: EbeguRestUtil;
    log: ILogService;

    static $inject = ['$http', 'REST_API', 'EbeguRestUtil', '$log'];
    /* @ngInject */
    constructor($http: IHttpService, private REST_API: string, ebeguRestUtil: EbeguRestUtil, $log: ILogService) {
        this.serviceURL = REST_API + 'pendenzen/steueramt';
        this.http = $http;
        this.ebeguRestUtil = ebeguRestUtil;
        this.log = $log;
    }

    public getServiceName(): string {
        return 'PendenzSteueramtRS';
    }

    public getPendenzenList(): IPromise<Array<TSAntragDTO>> {
        return this.http.get(this.serviceURL)
            .then((response: any) => {
                this.log.debug('PARSING pendenzenSteueramt REST object ', response.data);
                return this.ebeguRestUtil.parseAntragDTOs(response.data);
            });
    }
}
