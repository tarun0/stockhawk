package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.model.Quote;
import com.sam_chordas.android.stockhawk.model.RequestModel;
import com.sam_chordas.android.stockhawk.service.RetrieveHistoryService;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class DetailsChartActivity extends Activity {
    Intent intent;
    String query;
    ArrayList<Float> prices;
    ArrayList<String> dates;
    BarChart chart;
    BarDataSet barDataSet;
    BarData barData;
    ArrayList<BarEntry> entries;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_chart);
        chart = (BarChart) findViewById(R.id.barchart);
        intent = getIntent();
        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(intent.getStringExtra("symbol"));
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }

        //if savedInstanceState is null
        if (savedInstanceState == null) {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.progress_dialog_message));
            RestAdapter builder = new RestAdapter.Builder()
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .setEndpoint("https://query.yahooapis.com/v1/public")
                    .build();

            query = "select * from yahoo.finance.historicaldata where symbol = \'" +
                    intent.getStringExtra("symbol") +
                    "\' and startDate = \'2009-09-11\' and endDate = \'2010-03-10\'";
            ConnectivityManager cm =
                    (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            if (isConnected) {
                dialog.show();
                RetrieveHistoryService retrieveHistoryService = builder.create(RetrieveHistoryService.class);
                retrieveHistoryService.getHistory(query, new Callback<RequestModel>() {
                    @Override
                    public void success(RequestModel retrievedResponse, Response response) {
                        dialog.dismiss();
                        if (retrievedResponse.getQuery().getCount() > 0) {

                            Toast.makeText(getApplicationContext(), R.string.pinch_to_zoom, Toast.LENGTH_SHORT).show();
                            int total = retrievedResponse.getQuery().getCount();
                            prices = new ArrayList<>();
                            dates = new ArrayList<String>();

                            ArrayList<Quote> quoteList = retrievedResponse.getQuery().getResults().getQuote();
                            entries = new ArrayList<BarEntry>();

                            for (int i = 0; i < total; i++) {
                                prices.add(quoteList.get(i).getClose());
                                dates.add(quoteList.get(i).getDate());
                                entries.add(new BarEntry(prices.get(i), i));
                            }

                            makeChart(dates, prices);

                        } else {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(DetailsChartActivity.this);
                            alertDialog.setTitle(R.string.message).setMessage(R.string.no_history_data_available).setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent intent = new Intent(getApplicationContext(), MyStocksActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        }
                                    }).show();
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        dialog.dismiss();
                        AlertDialog.Builder alert = new AlertDialog.Builder(DetailsChartActivity.this);
                        alert.setTitle(R.string.message).setMessage(R.string.retrieve_chart_network_issue);
                        alert.setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(getApplicationContext(), MyStocksActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }).show();
                    }
                });
            }
            else {
                Toast.makeText(this, R.string.network_toast, Toast.LENGTH_SHORT).show();
            }
        }
        else {
            //make chart
            dates = savedInstanceState.getStringArrayList("dates_array");
            prices = floatToArrayList(savedInstanceState.getFloatArray("prices_array"));
            makeChart(dates, prices);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("dates_array", dates);
        outState.putFloatArray("prices_array", arrayListToArray(prices));
    }

    void makeChart (ArrayList<String> date, ArrayList<Float> price) {
        entries = new ArrayList<BarEntry>();
        for (int i=0; i<date.size(); i++) {
            entries.add(new BarEntry(price.get(i), i));
        }
        barDataSet = new BarDataSet(entries, "Close Price");
        barData = new BarData(date, barDataSet);
        chart.setData(barData);
        chart.setDescription("Close price vs Date");
        chart.animateY(1500);
        chart.invalidate();
    }

    float[] arrayListToArray(ArrayList<Float> list) {
        float[] array = new float[list.size()];
        for (int i=0; i<list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    ArrayList<Float> floatToArrayList (float[] array) {
        ArrayList<Float> list = new ArrayList<>();
        for (int i=0; i<array.length; i++) {
            list.add(array[i]);
        }
        return list;
    }
}
