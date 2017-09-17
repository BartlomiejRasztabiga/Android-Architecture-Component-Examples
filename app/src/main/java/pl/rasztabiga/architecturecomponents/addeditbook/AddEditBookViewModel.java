package pl.rasztabiga.architecturecomponents.addeditbook;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.support.annotation.Nullable;

import pl.rasztabiga.architecturecomponents.R;
import pl.rasztabiga.architecturecomponents.SingleLiveEvent;
import pl.rasztabiga.architecturecomponents.SnackbarMessage;
import pl.rasztabiga.architecturecomponents.books.BooksRepository;
import pl.rasztabiga.architecturecomponents.books.persistence.Book;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksDataSource;

public class AddEditBookViewModel extends AndroidViewModel implements BooksDataSource.GetBookCallback {

    public final ObservableField<String> title = new ObservableField<>();

    public final ObservableField<Long> pages = new ObservableField<>();

    public final ObservableBoolean dataLoading = new ObservableBoolean(false);

    private final SnackbarMessage mSnackbarText = new SnackbarMessage();

    private final SingleLiveEvent<Void> mBookUpdated = new SingleLiveEvent<>();

    private final BooksRepository mBooksRepository;

    @Nullable
    private Long mBookId;

    private boolean mIsNewBook;

    private boolean mIsDataLoaded = false;

    private boolean mBookCompleted = false;

    public AddEditBookViewModel(Application context,
                                BooksRepository booksRepository) {
        super(context);
        mBooksRepository = booksRepository;
    }

    public void start(Long bookId) {
        if (dataLoading.get()) {
            // Already loading, ignore
            return;
        }
        mBookId = bookId;
        if (bookId == null) {
            // No need to populate, it's a new task
            mIsNewBook = true;
            return;
        }
        if (mIsDataLoaded) {
            // No need to populate, already have data
            return;
        }
        mIsNewBook = false;
        dataLoading.set(true);

        mBooksRepository.getBook(bookId, this);
    }

    @Override
    public void onBookLoaded(Book book) {
        title.set(book.getTitle());
        pages.set(book.getPages());
        mBookCompleted = book.isCompleted();
        dataLoading.set(false);
        mIsDataLoaded = true;

        // Note that there's no need to notify that the values changed because we're using
        // ObservableFields.
    }

    @Override
    public void onDataNotAvailable() {
        dataLoading.set(false);
    }

    // Called when clicking on fab.
    void saveBook() {
        Book book = new Book(title.get(), pages.get());
        if (book.isEmpty()) {
            mSnackbarText.setValue(R.string.empty_book_message);
            return;
        }
        if (!mIsNewBook && mBookId != null) {
            book = new Book(mBookId, title.get(), pages.get(), mBookCompleted);
            updateBook(book);
        } else {
            saveBook(book);
        }


    }

    SnackbarMessage getSnackbarMessage() {
        return mSnackbarText;
    }

    SingleLiveEvent<Void> getBookUpdatedEvent() {
        return mBookUpdated;
    }

    private void updateBook(Book book) {
        mBooksRepository.updateBook(book);
        mBookUpdated.call();

        // Refresh to keep data consistent
        mBooksRepository.refreshBooks();
    }

    private void saveBook(Book book) {
        mBooksRepository.saveBook(book);
        mBookUpdated.call();

        // Refresh to keep data consistent
        mBooksRepository.refreshBooks();
    }
}
