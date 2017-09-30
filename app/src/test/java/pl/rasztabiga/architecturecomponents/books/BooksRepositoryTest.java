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
import static org.mockito.Mockito.atLeastOnce;
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
    private BooksDataSource.SaveBookCallback mSaveBooksCallback;

    @Mock
    private Context mContext;

    @Captor
    private ArgumentCaptor<BooksDataSource.GetBookCallback> mBookCallbackCaptor;

    @Captor
    private ArgumentCaptor<BooksDataSource.LoadBooksCallback> mBooksCallbackCaptor;

    @Captor
    private ArgumentCaptor<BooksDataSource.SaveBookCallback> mSaveBookCallbackCaptor;

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
    public void whenGetBooks_thenRepositoryCachesAfterFirstCall() {
        twoBooksLoadCallsToRepository(mLoadBooksCallback);

        verify(mBooksLocalDataSource, times(1)).getBooks(any(BooksDataSource.LoadBooksCallback.class));
        verify(mBooksRemoteDataSource, never()).getBooks(any(BooksDataSource.LoadBooksCallback.class));
    }

    @Test
    public void whenGetBooks_thenRequestsAllBooksFromLocalDataSource() {
        mBooksRepository.getBooks(mLoadBooksCallback);

        verify(mBooksLocalDataSource).getBooks(any(BooksDataSource.LoadBooksCallback.class));
    }

    @Test
    public void givenLocalNotAvailable_whenGetBooks_thenRequestsBooksFromRemoteDataSource() {
        mBooksRepository.getBooks(mLoadBooksCallback);

        setBooksNotAvailable(mBooksLocalDataSource);

        setBooksAvailable(mBooksRemoteDataSource, BOOKS);

        verify(mBooksRemoteDataSource).getBooks(any(BooksDataSource.LoadBooksCallback.class));
    }

    @Test
    public void givenUserRefresh_whenGetBooks_thenTriggersSynchroniseWithRemote() {
        // First call to cache books
        mBooksRepository.getBooks(mLoadBooksCallback);

        verify(mBooksLocalDataSource).getBooks(mBooksCallbackCaptor.capture());

        mBooksCallbackCaptor.getValue().onBooksLoaded(BOOKS);

        // Second call to trigger synchronise
        mBooksRepository.refreshBooks();

        mBooksRepository.getBooks(mLoadBooksCallback);

        // triggers synchronise
        verify(mBooksRemoteDataSource).deleteAllBooks();
        verify(mBooksRemoteDataSource, atLeastOnce()).saveBook(any(Book.class),
                any(BooksDataSource.SaveBookCallback.class));
    }

    @Test
    public void givenNewBook_whenSaveBook_thenSavesBooksToLocalDatabaseAndRemote() {
        Book newBook = new Book(BOOK_ID1, BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);

        mBooksRepository.saveBook(newBook);

        verify(mBooksLocalDataSource).saveBook(eq(newBook), mSaveBookCallbackCaptor.capture());

        mSaveBookCallbackCaptor.getValue().onBookSaved(BOOK_ID1);

        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);

        verify(mBooksRemoteDataSource).saveBook(eq(newBook), mSaveBookCallbackCaptor.capture());
    }

    @Test
    public void givenLocalNotAvailable_whenSaveBook_thenTriggersOnDataNotAvailable() {
        Book newBook = new Book(BOOK_ID1, BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);

        mBooksRepository.saveBook(newBook);

        verify(mBooksLocalDataSource).saveBook(eq(newBook), mSaveBookCallbackCaptor.capture());

        mSaveBookCallbackCaptor.getValue().onDataNotAvailable();
    }

    @Test
    public void whenUpdateBook_thenActivatesLocalAndRemoteAndUpdatesCache() {
        Book updatedBook = new Book(BOOK_ID1, BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1, true);
        mBooksRepository.saveBook(updatedBook);

        mBooksRepository.updateBook(updatedBook);

        verify(mBooksRemoteDataSource).updateBook(updatedBook);
        verify(mBooksLocalDataSource).updateBook(updatedBook);
        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);
        assertThat(mBooksRepository.mCachedBooks.get(updatedBook.getId()).isActive()).isFalse();
    }

    @Test
    public void whenCompleteBook_thenActivatesLocalAndRemoteAndUpdatesCache() {
        Book newBook = new Book(BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);
        mBooksRepository.saveBook(newBook);

        mBooksRepository.completeBook(newBook);

        verify(mBooksRemoteDataSource).completeBook(newBook);
        verify(mBooksLocalDataSource).completeBook(newBook);
        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);
        assertThat(mBooksRepository.mCachedBooks.get(newBook.getId()).isActive()).isFalse();
    }

    @Test
    public void whenCompleteBookWithId_thenActivatesLocalAndRemoteAndUpdatesCache() {
        Book newBook = new Book(BOOK_ID1, BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);
        mBooksRepository.saveBook(newBook);

        // Add new book to cache
        verify(mBooksLocalDataSource).saveBook(eq(newBook), mSaveBookCallbackCaptor.capture());
        mSaveBookCallbackCaptor.getValue().onBookSaved(BOOK_ID1);

        mBooksRepository.completeBook(newBook.getId());

        verify(mBooksRemoteDataSource).completeBook(newBook);
        verify(mBooksLocalDataSource).completeBook(newBook);
        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);
        assertThat(mBooksRepository.mCachedBooks.get(newBook.getId()).isActive()).isFalse();
    }

    @Test
    public void whenGetBook_thenRequestsSingleBookFromLocalDataSource() {
        mBooksRepository.getBook(BOOK_ID1, mGetBookCallback);

        verify(mBooksLocalDataSource).getBook(eq(BOOK_ID1), mBookCallbackCaptor.capture());
        mBookCallbackCaptor.getValue().onBookLoaded(BOOKS.get(0));
    }

    @Test
    public void givenCachedBook_whenGetThisBook_thenRespondsWithCache() {
        Book newBook = new Book(BOOK_ID1, BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);
        mBooksRepository.saveBook(newBook);

        // Add new book to cache
        verify(mBooksLocalDataSource).saveBook(eq(newBook), mSaveBookCallbackCaptor.capture());
        mSaveBookCallbackCaptor.getValue().onBookSaved(BOOK_ID1);

        mBooksRepository.getBook(BOOK_ID1, mGetBookCallback);

        verify(mBooksLocalDataSource, never()).getBook(BOOK_ID1, mGetBookCallback);
        verify(mBooksRemoteDataSource, never()).getBook(BOOK_ID1, mGetBookCallback);
    }

    @Test
    public void givenLocalUnavailable_whenGetBook_thenRequestsBookFromRemote() {
        mBooksRepository.getBook(BOOK_ID1, mGetBookCallback);

        setBookNotAvailable(mBooksLocalDataSource, BOOK_ID1);
        setBookAvailable(mBooksRemoteDataSource, BOOKS.get(0));

        verify(mBooksRemoteDataSource).getBook(eq(BOOK_ID1), mBookCallbackCaptor.capture());
    }

    @Test
    public void givenLocalAndRemoteUnavailable_whenGetBook_thenTriggersOnDataNotAvailable() {
        mBooksRepository.getBook(BOOK_ID1, mGetBookCallback);

        setBookNotAvailable(mBooksLocalDataSource, BOOK_ID1);
        setBookNotAvailable(mBooksRemoteDataSource, BOOK_ID1);

        verify(mGetBookCallback).onDataNotAvailable();
    }

    @Test
    public void whenDeleteAllBooks_thenDeleteBooksInLocalAndRemoteAndUpdatesCache(){
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
    public void whenDeleteBook_thenDeleteInLocalAndRemoteAndRemovesFromCache() {
        Book newBook = new Book(BOOK_ID1, BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);
        mBooksRepository.saveBook(newBook);

        // Add new book to cache
        verify(mBooksLocalDataSource).saveBook(eq(newBook), mSaveBookCallbackCaptor.capture());
        mSaveBookCallbackCaptor.getValue().onBookSaved(BOOK_ID1);

        assertThat(mBooksRepository.mCachedBooks.containsKey(newBook.getId())).isTrue();

        mBooksRepository.deleteBook(newBook.getId());

        verify(mBooksLocalDataSource).deleteBook(newBook.getId());
        verify(mBooksRemoteDataSource).deleteBook(newBook.getId());

        assertThat(mBooksRepository.mCachedBooks.containsKey(newBook.getId())).isFalse();
    }

    @Test
    public void getBooksWithLocalDataSourceUnavailable_booksAreRetrievedFromRemote(){

        mBooksRepository.getBooks(mLoadBooksCallback);

        setBooksNotAvailable(mBooksLocalDataSource);

        setBooksAvailable(mBooksRemoteDataSource, BOOKS);

        verify(mLoadBooksCallback).onBooksLoaded(BOOKS);
    }

    @Test
    public void getBooksWithLocalDataSourceAvailable_booksAreRetrievedFromLocal() {

        mBooksRepository.getBooks(mLoadBooksCallback);

        setBooksAvailable(mBooksLocalDataSource, BOOKS);

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
    public void getBookWithLocalDataSourceAvailable_bookIsRetrievedFromLocal() {

        mBooksRepository.getBook(BOOK_ID1, mGetBookCallback);

        setBookAvailable(mBooksLocalDataSource, BOOKS.get(0));

        verify(mGetBookCallback).onBookLoaded(BOOKS.get(0));
    }

    @Test
    public void getBookWithLocalDataSourceUnavailable_bookIsRetrievedFromRemote() {

        mBooksRepository.getBook(BOOK_ID1, mGetBookCallback);

        setBookNotAvailable(mBooksLocalDataSource, BOOK_ID1);

        setBookAvailable(mBooksRemoteDataSource, BOOKS.get(0));

        verify(mGetBookCallback).onBookLoaded(BOOKS.get(0));
    }

    @Test
    public void getBookWithLocalDataSourceUnavailableAndRemoteReturnsNull_firesOnDataUnavailable() {

        mBooksRepository.getBook(0L, mGetBookCallback);

        setBookNotAvailable(mBooksLocalDataSource, 0L);

        setBookAvailable(mBooksRemoteDataSource, null);

        verify(mGetBookCallback).onDataNotAvailable();
    }


    @Test
    public void getBookWithBothDataSourcesUnavailable_firesOnDataUnavailable(){
        final long bookId = 123L;

        mBooksRepository.getBook(bookId, mGetBookCallback);

        setBookNotAvailable(mBooksLocalDataSource, bookId);

        setBookNotAvailable(mBooksRemoteDataSource, bookId);

        verify(mGetBookCallback).onDataNotAvailable();
    }

    private void twoBooksLoadCallsToRepository(BooksDataSource.LoadBooksCallback callback) {

        // First call to cache books
        mBooksRepository.getBooks(callback);

        verify(mBooksLocalDataSource).getBooks(mBooksCallbackCaptor.capture());

        mBooksCallbackCaptor.getValue().onBooksLoaded(BOOKS);

        // Second call to verify that books were retrieved from cache
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
        if (book == null) {
            verify(dataSource).getBook(eq(0L), mBookCallbackCaptor.capture());
        } else {
            verify(dataSource).getBook(eq(book.getId()), mBookCallbackCaptor.capture());
        }
        mBookCallbackCaptor.getValue().onBookLoaded(book);
    }

}
