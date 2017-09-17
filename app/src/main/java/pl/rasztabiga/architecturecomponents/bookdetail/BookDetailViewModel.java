package pl.rasztabiga.architecturecomponents.bookdetail;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.support.annotation.StringRes;

import pl.rasztabiga.architecturecomponents.R;
import pl.rasztabiga.architecturecomponents.SingleLiveEvent;
import pl.rasztabiga.architecturecomponents.SnackbarMessage;
import pl.rasztabiga.architecturecomponents.books.BooksRepository;
import pl.rasztabiga.architecturecomponents.books.persistence.Book;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksDataSource;

public class BookDetailViewModel extends AndroidViewModel implements BooksDataSource.GetBookCallback {

    public final ObservableField<Book> book = new ObservableField<>();

    public final ObservableBoolean completed = new ObservableBoolean();

    private final SingleLiveEvent<Void> mEditBookCommand = new SingleLiveEvent<>();

    private final SingleLiveEvent<Void> mDeleteBookCommand = new SingleLiveEvent<>();

    private final BooksRepository mBooksRepository;

    private final SnackbarMessage mSnackbarText = new SnackbarMessage();

    private boolean mIsDataLoading;

    public BookDetailViewModel(Application context, BooksRepository booksRepository) {
        super(context);
        mBooksRepository = booksRepository;
    }

    public void deleteBook() {
        if (book.get() != null) {
            mBooksRepository.deleteBook(book.get().getId());
            mDeleteBookCommand.call();
        }
    }

    public void editBook() {
        mEditBookCommand.call();
    }

    public SnackbarMessage getSnackbarMessage() {
        return mSnackbarText;
    }

    public SingleLiveEvent<Void> getEditBookCommand() {
        return mEditBookCommand;
    }

    public SingleLiveEvent<Void> getDeleteBookCommand() {
        return mDeleteBookCommand;
    }

    public void setCompleted(boolean completed) {
        if (mIsDataLoading) {
            return;
        }
        Book bookToComplete = this.book.get();
        bookToComplete.setCompleted(completed);
        if (completed) {
            mBooksRepository.completeBook(bookToComplete);
            showSnackbarMessage(R.string.book_marked_complete);
        }
    }

    public void start(Long bookId) {
        if (bookId != null) {
            mIsDataLoading = true;
            mBooksRepository.getBook(bookId, this);
        }
    }

    public void setBook(Book book) {
        this.book.set(book);
        if (book != null) {
            completed.set(book.isCompleted());
        }
    }

    public boolean isDataAvailable() {
        return book.get() != null;
    }

    public boolean isDataLoading() {
        return mIsDataLoading;
    }

    @Override
    public void onBookLoaded(Book book) {
        setBook(book);
        mIsDataLoading = false;
    }

    @Override
    public void onDataNotAvailable() {
        book.set(null);
        mIsDataLoading = false;
    }

    public void onRefresh() {
        if (book.get() != null) {
            start(book.get().getId());
        }
    }

    private void showSnackbarMessage(@StringRes Integer message) {
        mSnackbarText.setValue(message);
    }
}
