package com.doosy.megaworxx.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.doosy.megaworxx.model.ClientToken;
import com.doosy.megaworxx.model.DataServerResponse;
import com.doosy.megaworxx.repository.ApiRepository;
import com.doosy.megaworxx.request.ServiceGenerator;

public class TokenViewModel extends ViewModel {

    private ApiRepository<ClientToken> mApiRepository;

    public TokenViewModel(){
        mApiRepository = new ApiRepository<>
                (ServiceGenerator.getCampaignApiToken().getToken("client_credentials",
                        "VehicleManagementAPI"));
    }

    public LiveData<DataServerResponse<ClientToken>> getResponse(){
        return mApiRepository.getTokenResponse();
    }

    public void processApi(){
        mApiRepository.processToken();
    }

}
