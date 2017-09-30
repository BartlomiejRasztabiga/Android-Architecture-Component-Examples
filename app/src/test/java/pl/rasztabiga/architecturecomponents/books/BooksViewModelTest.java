package pl.rasztabiga.architecturecomponents.books;

import android.app.Application;
import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.Observer;
import android.content.res.Resources;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import pl.rasztabiga.architecturecomponents.R;
import pl.rasztabiga.architecturecomponents.TestUtils;
import pl.rasztabiga.architecturecomponents.addeditbook.AddEditBookActivity;
import pl.rasztabiga.architecturecomponents.bookdetail.BookDetailActivity;
import pl.rasztabiga.architecturecomponents.books.persistence.Book;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksDataSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BooksViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    private static List<Book> BOOKS;

    @Mock
    private BooksRepository mBooksRepository;

    @Mock
    private Application mContext;

    @Captor
    private ArgumentCaptor<BooksDataSource.LoadBooksCallback> mLoadBooksCallbackCaptor;

    private BooksViewModel mBooksViewModel;

    @Before
    public void setupBooksViewModel() {
        MockitoAnnotations.initMocks(this);

        setupContext();

        mBooksViewModel = new BooksViewModel(mContext, mBooksRepository);

        BOOKS = Lists.newArrayList(
                new Book("Title 1", 100L),
                new Book("Title 2", 200L),
                new Book("Title 3", 300L)
        );

        mBooksViewModel.getSnackbarMessage().removeObservers(TestUtils.TEST_OBSERVER);
    }

    private void setupContext() {
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getString(R.string.successfully_saved_book_message))
                .thenReturn("EDIT_RESULT_OK");
        when(mContext.getString(R.string.successfully_added_book_message))
                .thenReturn("ADD_EDIT_RESULT_OK");
        when(mContext.getString(R.string.successfully_deleted_book_message))
                .thenReturn("DELETE_RESULT_OK");

        when(mContext.getResources()).thenReturn(mock(Resources.class));
    }

    @Test
    public void whenViewModelStarts_thenDataIsLoaded() {
        mBooksViewModel.start();

        verify(mBooksRepository).getBooks(mLoadBooksCallbackCaptor.capture());

        // Then progress indicator is shown
        assertThat(mBooksViewModel.dataLoading.get()).isTrue();
        mLoadBooksCallbackCaptor.getValue().onBooksLoaded(BOOKS);

        // Then progress indicator is hidden
        assertThat(mBooksViewModel.dataLoading.get()).isFalse();

        // And data loaded
        assertThat(mBooksViewModel.items.isEmpty()).isFalse();
        assertThat(mBooksViewModel.items.size()).isEqualTo(3);
    }

    @Test
    public void givenForceUpdateTrue_whenBooksAreLoadedFromRepo_thenDataIsLoaded() {
        mBooksViewModel.loadBooks(true);

        verify(mBooksRepository).refreshBooks();
        verify(mBooksRepository).getBooks(mLoadBooksCallbackCaptor.capture());
    }

    @Test
    public void givenDataUnavailable_whenLoadBooks_thenErrorIsShown() {
        mBooksViewModel.loadBooks(true);

        verify(mBooksRepository).getBooks(mLoadBooksCallbackCaptor.capture());
        mLoadBooksCallbackCaptor.getValue().onDataNotAvailable();

        assertThat(mBooksViewModel.dataLoading.get()).isFalse();

        assertThat(mBooksViewModel.getSnackbarMessage().getValue())
                .isEqualTo(R.string.no_internet);
    }

    @Test
    public void whenClickOnFab_thenShowsAddBookUI() {
        Observer<Void> observer = mock(Observer.class);

        mBooksViewModel.getNewBookEvent().observe(TestUtils.TEST_OBSERVER, observer);

        // When adding a new book
        mBooksViewModel.addNewBook();

        // Then the event is triggered
        verify(observer).onChanged(null);
    }

    @Test
    public void handleActivityResult_editOK() {
        // When BookDetailActivity sends a EDIT_RESULT_OK
        Observer<Integer> observer = mock(Observer.class);

        mBooksViewModel.getSnackbarMessage().observe(TestUtils.TEST_OBSERVER, observer);

        mBooksViewModel.handleActivityResult(
                AddEditBookActivity.REQUEST_CODE, BookDetailActivity.EDIT_RESULT_OK);

        // Then the snackbar shows the correct message
        verify(observer).onChanged(R.string.successfully_saved_book_message);
        assertThat(mBooksViewModel.getSnackbarMessage().getValue())
                .isEqualTo(R.string.successfully_saved_book_message);
    }

    @Test
    public void handleActivityResult_addEditOK() {
        // When AddEditBookActivity sends a ADD_EDIT_RESULT_OK
        Observer<Integer> observer = mock(Observer.class);

        mBooksViewModel.getSnackbarMessage().observe(TestUtils.TEST_OBSERVER, observer);

        mBooksViewModel.handleActivityResult(
                AddEditBookActivity.REQUEST_CODE, AddEditBookActivity.ADD_EDIT_RESULT_OK);

        // Then the snackbar shows the correct message
        verify(observer).onChanged(R.string.successfully_added_book_message);
        assertThat(mBooksViewModel.getSnackbarMessage().getValue())
                .isEqualTo(R.string.successfully_added_book_message);
    }

    @Test
    public void handleActivityResult_deleteOK() {
        // When AddEditBookActivity sends a DELETE_RESULT_OK
        Observer<Integer> observer = mock(Observer.class);

        mBooksViewModel.getSnackbarMessage().observe(TestUtils.TEST_OBSERVER, observer);

        mBooksViewModel.handleActivityResult(
                AddEditBookActivity.REQUEST_CODE, BookDetailActivity.DELETE_RESULT_OK);

        // Then the snackbar shows the correct message
        verify(observer).onChanged(R.string.successfully_deleted_book_message);
        assertThat(mBooksViewModel.getSnackbarMessage().getValue())
                .isEqualTo(R.string.successfully_deleted_book_message);
    }

}