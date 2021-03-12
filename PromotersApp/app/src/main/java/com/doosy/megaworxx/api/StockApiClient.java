package com.doosy.megaworxx.api;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.doosy.megaworxx.AppExecutors;
import com.doosy.megaworxx.entity.Stock;
import com.doosy.megaworxx.model.AddStockModel;
import com.doosy.megaworxx.model.CheckModel;
import com.doosy.megaworxx.model.DataServerResponse;
import com.doosy.megaworxx.model.ServerResponse;
import com.doosy.megaworxx.request.ServiceGenerator;
import com.doosy.megaworxx.util.Constants;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

public class StockApiClient {
    private static StockApiClient instance;

    private MutableLiveData<DataServerResponse<Stock>> dataResponse;
    private MutableLiveData<ServerResponse> mResponse;
    private StockRunnable mStockRunnable;
    private AddStockRunnable mAddStockRunnable;
    private PromoterStockRunnable mPromoterStockRunnable;

    public static StockApiClient getInstance(){
        if(instance == null){
            instance = new StockApiClient();
        }
        return instance;
    }

    private StockApiClient(){
        dataResponse = new MutableLiveData<>();
        mResponse = new MutableLiveData<>();
    }

    public LiveData<DataServerResponse<Stock>> getDataResponse(){
        return dataResponse;
    }

    public LiveData<ServerResponse> getResponse(){
        return mResponse;
    }

    public void fetchStock(String token,String campaignId){
        if(mStockRunnable != null){
            mStockRunnable = null;
        }

        mStockRunnable = new StockRunnable(token,campaignId);

        final Future handler = AppExecutors.getInstance().
                getNetworkIO().submit(mStockRunnable);

        AppExecutors.getInstance().getNetworkIO().schedule(new Runnable() {
            @Override
            public void run() {
                handler.cancel(true);
            }
        }, Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);

    }

    public void fetchPromoterStock(String token,String promoterId,String campaignId, String campaignLocationId){
        dataResponse = new MutableLiveData<>();

        if(mPromoterStockRunnable != null){
            mPromoterStockRunnable = null;
        }

        mPromoterStockRunnable = new PromoterStockRunnable(token, promoterId, campaignId, campaignLocationId);

        final Future handler = AppExecutors.getInstance().
                getNetworkIO().submit(mPromoterStockRunnable);

        AppExecutors.getInstance().getNetworkIO().schedule(new Runnable() {
            @Override
            public void run() {
                handler.cancel(true);
            }
        }, Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);

    }

    public void addStock(String token,List<AddStockModel> addStockModel) {
        mResponse = new MutableLiveData<>();
        if(mAddStockRunnable != null){
            mAddStockRunnable = null;
        }

        mAddStockRunnable = new AddStockRunnable(token, addStockModel);

        final Future handler = AppExecutors.getInstance().
                getNetworkIO().submit(mAddStockRunnable);

        AppExecutors.getInstance().getNetworkIO().schedule(new Runnable() {
            @Override
            public void run() {
                handler.cancel(true);
            }
        }, Constants.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private class StockRunnable implements Runnable {
        String mCampaignId;
        String token;
        private boolean cancelRequest = false;

        public StockRunnable(String token, String campaignId){
            this.cancelRequest = false;
            this.mCampaignId = campaignId;
            this.token = token;
        }

        @Override
        public void run() {

            try{

                Response response = fetchStocks(token, mCampaignId).execute();

                if(cancelRequest){
                    return;
                }
                Log.d(Constants.TAG,"Before if : "+response.body());
                if(response.code() == 200){

                    DataServerResponse<Stock> serverResponse = ((DataServerResponse<Stock>)(response.body()));

                    if(serverResponse != null){
                        dataResponse.postValue(serverResponse);
                        Log.d(Constants.TAG,"Array size: "+serverResponse.getDataList().size());
                        return;
                    }
                    Log.d(Constants.TAG,"Model: "+response.body());
                }

                dataResponse.postValue(null);

            }catch (Exception e){
                Log.d(Constants.TAG,"Exception: " + e.fillInStackTrace());
                dataResponse.postValue(null);
            }

        }
    }

    private class AddStockRunnable implements Runnable {
        List<AddStockModel> mAddStockModel;
        String token;
        private boolean cancelRequest = false;

        public AddStockRunnable(String token,List<AddStockModel> addStockModel){
            this.cancelRequest = false;
            this.mAddStockModel = addStockModel;
            this.token = token;
        }

        @Override
        public void run() {

            try{

                Response response = addStockCall(token, mAddStockModel).execute();

                if(cancelRequest){
                    return;
                }

                if(response.code() == 200){

                    ServerResponse serverResponse = ((ServerResponse)(response.body()));

                    if(serverResponse != null){
                        mResponse.postValue(serverResponse);
                        return;
                    }
                    Log.d(Constants.TAG,"Model: "+response.body());
                }

                mResponse.postValue(null);

            }catch (Exception e){
                Log.d(Constants.TAG,"Exception: " + e.fillInStackTrace());
                mResponse.postValue(null);
            }

        }
    }

    private class PromoterStockRunnable implements Runnable {
        String mPromoterId;
        String mCampaignId;
        String mCampaignLocationId;
        String mToken;

        private boolean cancelRequest = false;

        public PromoterStockRunnable(String token,String promoterId,String campaignId, String campaignLocationId){
            this.cancelRequest = false;
            mPromoterId = promoterId;
            mCampaignId = campaignId;
            mCampaignLocationId = campaignLocationId;
            mToken = token;
        }

        @Override
        public void run() {

            try{

                Response response = getPromoterStocks(mToken, mPromoterId, mCampaignId, mCampaignLocationId).execute();

                if(cancelRequest){
                    return;
                }
                Log.d(Constants.TAG,"Promoter Stock Before if : "+response.body());
                Log.d(Constants.TAG,response.body().toString());
                if(response.code() == 200){

                    DataServerResponse<Stock> serverResponse = ((DataServerResponse<Stock>)(response.body()));

                    if(serverResponse != null){
                        dataResponse.postValue(serverResponse);
                        return;
                    }
                    Log.d(Constants.TAG,"Promoter Stock Arrays Size: "+serverResponse.getDataList().size());
                }

                dataResponse.postValue(null);

            }catch (Exception e){
                Log.d(Constants.TAG,"Exception: " + e.fillInStackTrace());
                dataResponse.postValue(null);
            }

        }
    }


    private Call<ServerResponse> addStockCall(String token,List<AddStockModel> addStockModel){
        return ServiceGenerator.getPromoterApi().saveCampaignStock(token, addStockModel);
    }

    private Call<DataServerResponse<Stock>> fetchStocks(String token,String campaignId){

        return ServiceGenerator.getPromoterApi().fetchCampaignStock(token,campaignId);
    }
    private Call<DataServerResponse<Stock>> getPromoterStocks(String token,String promoterId,String campaignId, String campaignLocationId){

        return ServiceGenerator.getPromoterApi().getPromoterStocks(token, promoterId,campaignId,campaignLocationId);
    }

}