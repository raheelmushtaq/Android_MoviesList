package app.android.movieslist.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.ethanhua.skeleton.Skeleton;
import com.ethanhua.skeleton.SkeletonScreen;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;

import java.lang.reflect.Type;
import java.util.ArrayList;

import app.android.movieslist.Constants;
import app.android.movieslist.R;
import app.android.movieslist.adapters.MoviesListAdapter;
import app.android.movieslist.models.MovieModel;
import app.android.movieslist.utils.MessageUtil;
import app.android.movieslist.utils.Utils;
import app.android.movieslist.utils.fragment_utils.FragmentOp;
import app.android.movieslist.utils.toolbar_utils.ToolbarOp;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by harrisazam on 2/9/2019.
 */

public class HomeFragment extends ToolbarFragment implements View.OnClickListener {

    private Context context;
    private String TAG = this.getClass().getSimpleName();

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.swipeRefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.noResultView)
    View noResultView;

    @BindView(R.id.tv_TryAgain)
    Button tv_TryAgain;

    private MoviesListAdapter moviesListAdapter;
    private SkeletonScreen skeletonScreen;

    public static HomeFragment newInstance() {
        Bundle args = new Bundle();
        HomeFragment fragment = new HomeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        context = inflater.getContext();

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        initViews();
        getFilmsListAPICall();
    }

    /**
     * METHOD TO SHOW SKELETON VIEW WHILE API CALLING
     */
    private void showSkeletonView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        MoviesListAdapter adapter = new MoviesListAdapter();
        skeletonScreen = Skeleton.bind(recyclerView)
                .adapter(adapter)
                .shimmer(true)
                .angle(20)
                .frozen(false)
                .duration(1200)
                .count(10)
                .load(R.layout.layout_skeleton_item_movie)
                .show();
    }

    @Override
    public void refreshToolbar() {
        ToolbarOp.refreshToolbar(getView(), context, getString(R.string.str_movies_list),
                null, ToolbarOp.Theme.Dark, 0, null);
    }

    /**
     * METHOD TO INIT VIEWS
     */
    private void initViews() {

        tv_TryAgain.setOnClickListener(this);

        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (moviesListAdapter != null) {
                    moviesListAdapter = null;
                }
                getFilmsListAPICall();
            }
        });

        if (moviesListAdapter != null) {
            moviesListAdapter = null;
        }
    }

    /**
     * GET MOVIES LIST DATA API NETWORK CALL
     */
    private void getFilmsListAPICall() {
        if (!Utils.hasInternetConnection(context)) {
            showNoResult(true);
            MessageUtil.showSnackbar(context, getString(R.string.str_connection_error), true);
            return;
        }
        showSkeletonView();
        AndroidNetworking.get(Constants.API_END_POINT_FILMS)
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            Type listType = new TypeToken<ArrayList<MovieModel>>() {
                            }.getType();
                            ArrayList<MovieModel> userModelArrayList = new Gson().fromJson(response.toString(), listType);

                            populateRecyclerView(userModelArrayList);
                            Log.e(TAG, "" + response.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "" + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        showNoResult(true);
                        Log.e(TAG, "" + anError.toString());
                    }
                });
    }

    /**
     * METHOD TO STOP REFRESHING
     */
    private void stopRefreshing() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * METHOD TO SHOW/HIDE NO RESULT VIEW
     */
    private void showNoResult(boolean isShow) {
        if (!isShow && skeletonScreen != null) {
            skeletonScreen.hide();
        }
        noResultView.setVisibility(isShow ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setVisibility(isShow ? View.GONE : View.VISIBLE);
        stopRefreshing();
    }

    /**
     * METHOD TO POPULATE DATA IN RECYCLER VIEW
     */
    private void populateRecyclerView(final ArrayList<MovieModel> movieModels) {

        if (movieModels != null && movieModels.size() > 0) {
            showNoResult(false);
            if (moviesListAdapter == null) {
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setLayoutManager(new LinearLayoutManager(context,
                        LinearLayoutManager.VERTICAL, false));
                moviesListAdapter = new MoviesListAdapter(movieModels, context, recyclerView);
                recyclerView.setAdapter(moviesListAdapter);
                moviesListAdapter.setClickListener(new MoviesListAdapter.ClickListeners() {
                    @Override
                    public void onItemClick(View view, int position) {
                        switch (view.getId()) {
                            case R.id.rv_MovieCardParent:
                                FragmentOp.gotoNextFragment(context, DetailFragment.newInstance(moviesListAdapter.getItem(position)));
                                break;
                            case R.id.iv_locations:
                                FragmentOp.gotoNextFragment(context, LocationFragment.newInstance());
                                break;
                            case R.id.iv_people:
                                FragmentOp.gotoNextFragment(context, PeopleFragment.newInstance());
                                break;
                            case R.id.iv_species:
                                FragmentOp.gotoNextFragment(context, SpeciesFragment.newInstance(moviesListAdapter.getItem(position).getSpecies()[0] + ""));
                                break;
                            case R.id.iv_vehicles:
                                FragmentOp.gotoNextFragment(context, VehiclesFragment.newInstance());
                                break;
                        }
                    }
                });
            } else {
                //TODO OTHER WISE ADD DATA
            }
        } else {
            showNoResult(true);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_TryAgain:
                getFilmsListAPICall();
                break;
        }
    }
}
