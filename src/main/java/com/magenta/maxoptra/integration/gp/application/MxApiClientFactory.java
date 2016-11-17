package com.magenta.maxoptra.integration.gp.application;

import com.magenta.maxoptra.integration.api.MXApiClient;
import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.configuration.MaxoptraConf;

import javax.inject.Inject;

public class MxApiClientFactory {

    @Inject
    MXApiClient mxApiClient;

    public MXApiClient getMxApiClient(Account account) throws Exception {
        MaxoptraConf settings = account.maxoptra;
        mxApiClient.setSettings(settings);
        return mxApiClient;
    }
}
