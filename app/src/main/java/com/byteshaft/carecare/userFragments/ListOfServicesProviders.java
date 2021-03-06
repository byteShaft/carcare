package com.byteshaft.carecare.userFragments;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.byteshaft.carecare.Adapters.AutoMechanicServiceProvidersListAdapter;
import com.byteshaft.carecare.R;
import com.byteshaft.carecare.gettersetter.ServicesProvidersListItems;
import com.byteshaft.carecare.utils.AppGlobals;
import com.byteshaft.carecare.utils.Helpers;
import com.byteshaft.requests.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

public class ListOfServicesProviders extends Fragment implements HttpRequest.OnReadyStateChangeListener,
        HttpRequest.OnErrorListener {

    private View mBaseView;
    private ListView mServiceProvidersListView;
    private ArrayList<ServicesProvidersListItems> arrayList;
    private AutoMechanicServiceProvidersListAdapter adapter;
    private HttpRequest request;
    private ArrayList<Integer> mServicesIdArrayList;
    public static String mLocationString;
    private String mServiceString;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBaseView = inflater.inflate(R.layout.fragment_service_providers, container, false);
        mServiceProvidersListView = mBaseView.findViewById(R.id.service_providers_list_view);
        arrayList = new ArrayList<>();
        mServicesIdArrayList = new ArrayList<>();
        adapter = new AutoMechanicServiceProvidersListAdapter(getActivity(), arrayList);
        mServiceProvidersListView.setAdapter(adapter);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mServicesIdArrayList = (ArrayList<Integer>) getArguments().getSerializable("service_id");
            Log.e("mServicesIdArrayList", " " + mServicesIdArrayList);
            mLocationString = bundle.getString("location");
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mServicesIdArrayList.size(); i++) {
            stringBuilder.append("&service=" + mServicesIdArrayList.get(i));
            stringBuilder.append("&");
        }
        mServiceString = stringBuilder.toString();
        mServiceString = mServiceString.substring(0, mServiceString.length() - 1);
        Log.e("StringBuilder", " " + mServiceString);
        getServiceProvidersList(mLocationString);
        return mBaseView;
    }

    @Override
    public void onReadyStateChange(HttpRequest httpRequest, int readyState) {
        switch (readyState) {
            case HttpRequest.STATE_DONE:
                Helpers.dismissProgressDialog();
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        Log.e("working ", " " + httpRequest.getResponseText());
                        break;
                    case HttpURLConnection.HTTP_OK:
                        try {
                            JSONObject mainJsonObject = new JSONObject(httpRequest.getResponseText());
                            JSONArray jsonArray = mainJsonObject.getJSONArray("results");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                Log.e("working ", " " + jsonArray.getJSONObject(i));
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                ServicesProvidersListItems servicesProvidersListItems = new ServicesProvidersListItems();
                                servicesProvidersListItems.setServiceProviderId(jsonObject.getInt("id"));
                                servicesProvidersListItems.setServiceProviderName(jsonObject.getString("name"));
                                servicesProvidersListItems.setServiceProviderLocation(jsonObject.getString("address_coordinates"));
                                Log.e("working ", " " + jsonObject.getString("address_coordinates"));
                                servicesProvidersListItems.setServiceProviderImage(jsonObject.getString("profile_photo"));
                                JSONObject profileJsonObject = jsonObject.getJSONObject("profile");
                                servicesProvidersListItems.setServiceProviderExperience(profileJsonObject.getString("experience"));
                                servicesProvidersListItems.setServiceProviderStartTime(profileJsonObject.getString("start_time"));
                                servicesProvidersListItems.setServiceProviderEndTime(profileJsonObject.getString("end_time"));
                                arrayList.add(servicesProvidersListItems);
                                adapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                }
        }

    }

    @Override
    public void onError(HttpRequest httpRequest, int readyState, short i1, Exception exception) {
        Helpers.dismissProgressDialog();
        switch (readyState) {
            case HttpRequest.ERROR_CONNECTION_TIMED_OUT:
                Helpers.showSnackBar(getView(), getString(R.string.connection_time_out));
                break;
            case HttpRequest.ERROR_NETWORK_UNREACHABLE:
                Helpers.showSnackBar(getView(), exception.getLocalizedMessage());
                break;
        }

    }

    private void getServiceProvidersList(String baseLocation) {
        request = new HttpRequest(getActivity());
        request.setOnReadyStateChangeListener(this);
        request.setOnErrorListener(this);
        request.open("GET", String.format("%sfind-mechanic?base_location=%s", AppGlobals.BASE_URL,
                baseLocation + mServiceString));
        System.out.println(String.format("%sfind-mechanic?base_location=%s", AppGlobals.BASE_URL,
                baseLocation + mServiceString));
        request.setRequestHeader("Authorization", "Token " +
                AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_TOKEN));
        request.send();
        Helpers.showProgressDialog(getActivity(), getString(R.string.parts_list));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getActivity(), "Permission granted", Toast.LENGTH_SHORT).show();
                    adapter.callAction();
                } else {
                    Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}
