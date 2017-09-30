package pl.rasztabiga.architecturecomponents.addeditbook;

import android.app.Application;
import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.Observer;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddEditBookViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    private static Book BOOK;

    @Mock
    private BooksRepository mBooksRepository;

    @Mock
    private Application mContext;

    @Captor
    private ArgumentCaptor<BooksDataSource.GetBookCallback> mGetBookCallbackCaptor;

    @Captor
    private ArgumentCaptor<BooksDataSource.SaveBookCallback> mSaveBookCallbackCaptor;

    private AddEditBookViewModel mViewModel;

    @Before
    public void setupAddEditBookViewModel() {
        MockitoAnnotations.initMocks(this);

        setupContext();

        mViewModel = new AddEditBookViewModel(mContext, mBooksRepository);

        BOOK = new Book(1L, "Title 1", 100L, false);

        mViewModel.getSnackbarMessage().removeObservers(TestUtils.TEST_OBSERVER);
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
    public void givenNullBookId_whenStartViewModel_thenRepositoryIsNotTriggered() {
        mViewModel.start(null);

        verify(mBooksRepository, never()).getBook(any(), eq(mViewModel));
        assertThat(mViewModel.mIsNewBook).isTrue();
    }

    @Test
    public void givenDataLoadingTrue_whenStartViewModel_thenNothingExecutes() {
        mViewModel.dataLoading.set(true);
        mViewModel.start(null);

        assertThat(mViewModel.mIsNewBook).isFalse();
        assertThat(mViewModel.dataLoading.get()).isTrue();
        verify(mBooksRepository, never()).getBook(any(), eq(mViewModel));
    }

    @Test
    public void givenIsDataLoadedTrue_whenStartViewModel_thenNothingExecutes() {
        mViewModel.mIsDataLoaded = true;
        mViewModel.start(1L);

        assertThat(mViewModel.mIsDataLoaded).isTrue();
        verify(mBooksRepository, never()).getBook(any(), eq(mViewModel));
    }

    @Test
    public void whenStartViewModel_thenDataIsLoadedFromRepository() {
        mViewModel.start(1L);

        verify(mBooksRepository).getBook(eq(1L), mGetBookCallbackCaptor.capture());

        // Then progress indicator is shown
        assertThat(mViewModel.mIsNewBook).isFalse();
        assertThat(mViewModel.dataLoading.get()).isTrue();

        mGetBookCallbackCaptor.getValue().onBookLoaded(BOOK);

        // Then progress indicator is hidden
        assertThat(mViewModel.dataLoading.get()).isFalse();

        //And data loaded
        assertThat(mViewModel.title.get()).isEqualTo(BOOK.getTitle());
        assertThat(mViewModel.pages.get()).isEqualTo(BOOK.getPages());
        assertThat(mViewModel.mBookCompleted).isEqualTo(BOOK.isCompleted());
        assertThat(mViewModel.dataLoading.get()).isFalse();
        assertThat(mViewModel.mIsDataLoaded).isTrue();
    }

    @Test
    public void givenDataUnavailable_whenStartViewModel_thenDataLoadingIsFalse() {
        mViewModel.start(1L);

        verify(mBooksRepository).getBook(eq(1L), mGetBookCallbackCaptor.capture());

        mGetBookCallbackCaptor.getValue().onDataNotAvailable();

        assertThat(mViewModel.dataLoading.get()).isFalse();
    }

    @Test
    public void givenNewBook_whenFabIsClicked_thenCallsBookUpdatedEvent() {
        Book bookToSave = new Book("Title 1", 100L);

        Observer<Void> observer = mock(Observer.class);

        mViewModel.getBookUpdatedEvent().observe(TestUtils.TEST_OBSERVER, observer);

        // When adding new book
        mViewModel.title.set(bookToSave.getTitle());
        mViewModel.pages.set(bookToSave.getPages());
        mViewModel.saveBook();

        verify(mBooksRepository).saveBook(eq(bookToSave), mSaveBookCallbackCaptor.capture());

        mSaveBookCallbackCaptor.getValue().onBookSaved(anyLong());

        // Then the event is triggered
        verify(observer).onChanged(null);
    }

    @Test
    public void givenNewEmptyBook_whenFabIsClicked_thenShowsSnackbarTextEmptyBook() {
        mViewModel.saveBook();

        assertThat(mViewModel.mSnackbarText.getValue()).isEqualTo(R.string.empty_book_message);
    }

    @Test
    public void givenExistingBook_whenFabIsClicked_thenTriggersRepoToUpdateBook() {
        mViewModel.start(1L);
        mViewModel.mIsNewBook = false;
        mViewModel.title.set(BOOK.getTitle());
        mViewModel.pages.set(BOOK.getPages());

        mViewModel.saveBook();

        verify(mBooksRepository).updateBook(any(Book.class));
        verify(mBooksRepository).refreshBooks();
    }

}