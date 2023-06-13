package io.helidon.dbclient;

class DbClientServiceChainImpl implements DbClientService.Chain {
    private final DbClientService.Chain next;
    private final DbClientService service;

    DbClientServiceChainImpl(DbClientService.Chain next, DbClientService service) {
        this.next = next;
        this.service = service;
    }

    @Override
    public Object proceed(DbClientServiceContext context) {
        return service.handle(next, context);
    }
}