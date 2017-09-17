package pl.rasztabiga.architecturecomponents.books;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import pl.rasztabiga.architecturecomponents.R;
import pl.rasztabiga.architecturecomponents.ScrollChildSwipeRefreshLayout;
import pl.rasztabiga.architecturecomponents.SnackbarMessage;
import pl.rasztabiga.architecturecomponents.databinding.BooksFragBinding;
import pl.rasztabiga.architecturecomponents.util.SnackbarUtils;

public class BooksFragment extends Fragment {

    private BooksViewModel mBooksViewModel;

    private BooksFragBinding mBooksFragBinding;

    public BooksFragment() {
        // Requires empty public constructor
    }

    public static BooksFragment newInstance() {
        return new BooksFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        mBooksViewModel.start();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBooksFragBinding = BooksFragBinding.inflate(inflater, container, false);

        mBooksViewModel = BooksActivity.obtainViewModel(getActivity());

        mBooksFragBinding.setViewmodel(mBooksViewModel);

        setHasOptionsMenu(true);

        return mBooksFragBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupSnackbar();

        setupFab();

        setupListAdapter();

        setupRefreshLayout();
    }

    private void setupSnackbar() {
        mBooksViewModel.getSnackbarMessage()
                .observe(this, (SnackbarMessage.SnackbarObserver) snackbarMessageResourceId ->
                        SnackbarUtils.showSnackbar(getView(), getString(snackbarMessageResourceId)));
    }

    private void setupFab() {
        FloatingActionButton fab = getActivity().findViewById(R.id.fab_add_task);

        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(v -> mBooksViewModel.addNewBook());
    }

    private void setupListAdapter() {
        ListView listView = mBooksFragBinding.booksList;

        BooksAdapter mListAdapter = new BooksAdapter(
                new ArrayList<>(0),
                mBooksViewModel
        );
        listView.setAdapter(mListAdapter);
    }

    private void setupRefreshLayout() {
        ListView listView = mBooksFragBinding.booksList;
        final ScrollChildSwipeRefreshLayout swipeRefreshLayout = mBooksFragBinding.refreshLayout;
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.colorPrimary),
                ContextCompat.getColor(getActivity(), R.color.colorAccent),
                ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark)
        );
        // Set the scrolling view in the custom SwipeRefreshLayout.
        swipeRefreshLayout.setScrollUpChild(listView);
    }
}
