package pl.rasztabiga.architecturecomponents.bookdetail;

import android.app.Application;
import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.content.res.Resources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pl.rasztabiga.architecturecomponents.R;
import pl.rasztabiga.architecturecomponents.TestUtils;
import pl.rasztabiga.architecturecomponents.books.BooksRepository;
import pl.rasztabiga.architecturecomponents.books.persistence.Book;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksDataSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookDetailViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    private static Book BOOK;

    @Mock
    private BooksRepository mBooksRepository;

    @Mock
    private Application mContext;

    @Mock
    private BooksDataSource.GetBookCallback mRepositoryCallback;

    @Mock
    private  BooksDataSource.GetBookCallback mViewModelCallback;

    @Captor
    private ArgumentCaptor<BooksDataSource.GetBookCallback> mGetBookCallbackCaptor;

    private BookDetailViewModel mViewModel;

    @Before
    public void setupBookDetailViewModel() {
        MockitoAnnotations.initMocks(this);

        setupContext();

        mViewModel = new BookDetailViewModel(mContext, mBooksRepository);

        BOOK = new Book(1L, "Title 1", 100L, false);

        mViewModel.getSnackbarMessage().removeObservers(TestUtils.TEST_OBSERVER);
    }

    private void setupContext() {
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getResources()).thenReturn(mock(Resources.class));
    }

    @Test
    public void whenViewModelStarts_thenBookIsLoaded() {
        mViewModel.start(BOOK.getId());
        assertThat(mViewModel.isDataLoading()).isTrue();

        setupViewModelRepositoryCallback();

        assertThat(mViewModel.book.get().getTitle()).isEqualTo(BOOK.getTitle());
        assertThat(mViewModel.book.get().getPages()).isEqualTo(BOOK.getPages());
    }

    @Test
    public void givenNullBookID_whenViewModelStarts_thenNothingIsExecuted() {
        mViewModel.start(null);

        verify(mBooksRepository, never()).getBook(eq(1L), mGetBookCallbackCaptor.capture());
    }

    @Test
    public void whenCompleteBook_thenRepositoryIsTriggered() {
        setupViewModelRepositoryCallback();

        // When the ViewModel is asked to complete the book
        mViewModel.setCompleted(true);

        // Then request is sent to the book repository and the UI is updated
        verify(mBooksRepository).completeBook(BOOK);
        assertThat(mViewModel.getSnackbarMessage().getValue()).isEqualTo(R.string.book_updated);
    }

    @Test
    public void whenDeleteTask_thenRepositoryIsTriggered() {
        setupViewModelRepositoryCallback();

        // When the deletion of a task is requested
        mViewModel.deleteBook();

        // Then the repository is notified
        verify(mBooksRepository).deleteBook(BOOK.getId());
    }

    @Test
    public void givenDataUnavailable_whenGetTask_thenTriggersOnDataNotAvailable() {
        mViewModelCallback = mock(BooksDataSource.GetBookCallback.class);

        mViewModel.start(BOOK.getId());

        verify(mBooksRepository).getBook(eq(BOOK.getId()), mGetBookCallbackCaptor.capture());

        mGetBookCallbackCaptor.getValue().onDataNotAvailable();

        assertThat(mViewModel.isDataAvailable()).isFalse();
    }

    private void setupViewModelRepositoryCallback() {
        // Given an initialized ViewModel with an active book
        mViewModelCallback = mock(BooksDataSource.GetBookCallback.class);

        mViewModel.start(BOOK.getId());

        // Use a captor to get a reference for the callback.
        verify(mBooksRepository, atLeastOnce()).getBook(eq(BOOK.getId()), mGetBookCallbackCaptor.capture());

        mGetBookCallbackCaptor.getValue().onBookLoaded(BOOK); // Trigger callback
    }
}