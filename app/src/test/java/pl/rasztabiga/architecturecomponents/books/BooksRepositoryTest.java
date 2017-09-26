package pl.rasztabiga.architecturecomponents.books;


import android.content.Context;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import pl.rasztabiga.architecturecomponents.books.persistence.Book;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksDataSource;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksLocalDataSource;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksRemoteDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BooksRepositoryTest {

    private static final String BOOK_TITLE1 = "Book1";
    private static final String BOOK_TITLE2 = "Book2";
    private static final String BOOK_TITLE3 = "Book3";
    private static final long BOOK_NUMBER_OF_PAGES1 = 300L;
    private static final long BOOK_NUMBER_OF_PAGES2 = 20L;
    private static final long BOOK_NUMBER_OF_PAGES3 = 30000L;
    private static final long BOOK_ID1 = 1L;
    private static final long BOOK_ID2 = 2L;

    private static List<Book> BOOKS = Lists.newArrayList(new Book(BOOK_ID1, BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1),
            new Book(BOOK_ID2, BOOK_TITLE2, BOOK_NUMBER_OF_PAGES2));


    private BooksRepository mBooksRepository;

    @Mock
    private BooksLocalDataSource mBooksLocalDataSource;

    @Mock
    private BooksRemoteDataSource mBooksRemoteDataSource;

    @Mock
    private BooksDataSource.GetBookCallback mGetBookCallback;

    @Mock
    private BooksDataSource.LoadBooksCallback mLoadBooksCallback;

    @Mock
    private Context mContext;

    @Captor
    private ArgumentCaptor<BooksDataSource.GetBookCallback> mBookCallbackCaptor;

    @Captor
    private ArgumentCaptor<BooksDataSource.LoadBooksCallback> mBooksCallbackCaptor;

    @Before
    public void setupBooksRepository() {
        MockitoAnnotations.initMocks(this);

        mBooksRepository = BooksRepository.getInstance(
                mBooksRemoteDataSource, mBooksLocalDataSource);
    }

    @After
    public void destroyBooksRepository() {
        BooksRepository.destroyInstance();
    }

    @Test
    public void getBooks_repositoryCachesAfterFirstApiCall() {

        twoBooksLoadCallsToRepository(mLoadBooksCallback);

        verify(mBooksRemoteDataSource).getBooks(any(BooksDataSource.LoadBooksCallback.class));
    }

    @Test
    public void getBooks_requestsAllBooksFromLocalDataSource() {
        mBooksRepository.getBooks(mLoadBooksCallback);

        verify(mBooksLocalDataSource).getBooks(any(BooksDataSource.LoadBooksCallback.class));
    }

    @Test
    public void saveBooks_savesBooksToServiceAPI() {
        Book newBook = new Book(BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);

        mBooksRepository.saveBook(newBook);

        verify(mBooksRemoteDataSource).saveBook(newBook);
        verify(mBooksLocalDataSource).saveBook(newBook);

        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);

    }

    @Test
    public void completeBook_activatesBookToServiceAPIUpdatesCache() {
        Book newBook = new Book(BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);
        mBooksRepository.saveBook(newBook);

        mBooksRepository.completeBook(newBook);

        verify(mBooksRemoteDataSource).completeBook(newBook);
        verify(mBooksLocalDataSource).completeBook(newBook);
        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);
        assertThat(mBooksRepository.mCachedBooks.get(newBook.getId()).isActive()).isFalse();
    }

    @Test
    public void completeBookId_activatesBookToServiceAPIUpdatesCache() {
        Book newBook = new Book(BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);
        mBooksRepository.saveBook(newBook);

        mBooksRepository.completeBook(newBook.getId());

        verify(mBooksRemoteDataSource).completeBook(newBook);
        verify(mBooksLocalDataSource).completeBook(newBook);
        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);
        assertThat(mBooksRepository.mCachedBooks.get(newBook.getId()).isActive()).isFalse();
    }

    @Test
    public void getBook_requestsSingleBookFromLocalDataSource() {
        mBooksRepository.getBook(BOOK_ID1, mGetBookCallback);

        verify(mBooksLocalDataSource).getBook(eq(BOOK_ID1),
                any(BooksDataSource.GetBookCallback.class));
    }

    @Test
    public void deleteAllBooks_deleteBooksToServiceAPIUpdatesCache(){
        Book newBook1 = new Book(BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);
        mBooksRepository.saveBook(newBook1);
        Book newBook2 = new Book(BOOK_ID1, BOOK_TITLE2, BOOK_NUMBER_OF_PAGES2);
        mBooksRepository.saveBook(newBook2);
        Book newBook3 = new Book(BOOK_ID2, BOOK_TITLE3, BOOK_NUMBER_OF_PAGES3, true);
        mBooksRepository.saveBook(newBook3);

        mBooksRepository.deleteAllBooks();

        verify(mBooksLocalDataSource).deleteAllBooks();
        verify(mBooksRemoteDataSource).deleteAllBooks();

        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(0);
    }

    @Test
    public void deleteBook_deleteBookToServiceAPIRemovedFromCache(){
        Book newBook = new Book(BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);
        mBooksRepository.saveBook(newBook);
        assertThat(mBooksRepository.mCachedBooks.containsKey(newBook.getId())).isTrue();

        mBooksRepository.deleteBook(newBook.getId());

        verify(mBooksLocalDataSource).deleteBook(newBook.getId());
        verify(mBooksRemoteDataSource).deleteBook(newBook.getId());

        assertThat(mBooksRepository.mCachedBooks.containsKey(newBook.getId())).isFalse();
    }

    @Test
    public void getBooksWithDirtyCache_booksAreRetrievedFromRemote(){
        mBooksRepository.refreshBooks();
        mBooksRepository.getBooks(mLoadBooksCallback);

        setBooksAvailable(mBooksRemoteDataSource, BOOKS);

        verify(mBooksLocalDataSource, never()).getBooks(mLoadBooksCallback);
        verify(mLoadBooksCallback).onBooksLoaded(BOOKS);
    }



    @Test
    public void getBooksWithLocalDataSourceUnavailable_booksAreRetrievedFromRemote(){

        mBooksRepository.getBooks(mLoadBooksCallback);

        setBooksNotAvailable(mBooksLocalDataSource);

        setBooksAvailable(mBooksRemoteDataSource, BOOKS);

        verify(mLoadBooksCallback).onBooksLoaded(BOOKS);
    }

    @Test
    public void getBooksWithBothDataSourcesUnavailable_firesOnDataUnavailable(){
        mBooksRepository.getBooks(mLoadBooksCallback);

        setBooksNotAvailable(mBooksLocalDataSource);

        setBooksNotAvailable(mBooksRemoteDataSource);

        verify(mLoadBooksCallback).onDataNotAvailable();
    }


    @Test
    public void getBookWithBothDataSourcesUnavailable_firesOnDataUnavailable(){
        final long bookId = 123L;

        mBooksRepository.getBook(bookId, mGetBookCallback);

        setBookNotAvailable(mBooksLocalDataSource, bookId);

        setBookNotAvailable(mBooksRemoteDataSource, bookId);

        verify(mGetBookCallback).onDataNotAvailable();
    }


    @Test
    public void getBooks_refreshesLocalDataSource(){

        mBooksRepository.refreshBooks();

        mBooksRepository.getBooks(mLoadBooksCallback);

        setBooksAvailable(mBooksRemoteDataSource, BOOKS);

        verify(mBooksLocalDataSource, times(BOOKS.size())).saveBook(any(Book.class));

    }
    @Test
    public void updateBook_updatesCachedBook(){
        Book newBook = new Book(0L, "Book1", 100L);

        mBooksRepository.updateBook(newBook);

        verify(mBooksLocalDataSource).updateBook(newBook);
        verify(mBooksRemoteDataSource).updateBook(newBook);
        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);
    }

    private void twoBooksLoadCallsToRepository(BooksDataSource.LoadBooksCallback callback) {

        mBooksRepository.getBooks(callback);

        verify(mBooksLocalDataSource).getBooks(mBooksCallbackCaptor.capture());

        mBooksCallbackCaptor.getValue().onDataNotAvailable();

        verify(mBooksRemoteDataSource).getBooks(mBooksCallbackCaptor.capture());

        mBooksCallbackCaptor.getValue().onBooksLoaded(BOOKS);

        mBooksRepository.getBooks(callback);
    }

    private void setBooksNotAvailable(BooksDataSource booksDataSource){
        verify(booksDataSource).getBooks(mBooksCallbackCaptor.capture());
        mBooksCallbackCaptor.getValue().onDataNotAvailable();
    }

    private void setBooksAvailable(BooksDataSource dataSource, List<Book> books){
        verify(dataSource).getBooks(mBooksCallbackCaptor.capture());
        mBooksCallbackCaptor.getValue().onBooksLoaded(books);
    }

    private void setBookNotAvailable(BooksDataSource dataSource, long bookId){
        verify(dataSource).getBook(eq(bookId), mBookCallbackCaptor.capture());
        mBookCallbackCaptor.getValue().onDataNotAvailable();
    }
    private void setBookAvailable(BooksDataSource dataSource, Book book){
        verify(dataSource).getBook(book.getId(), mBookCallbackCaptor.capture());
        mBookCallbackCaptor.getValue().onBookLoaded(book);
    }

}
