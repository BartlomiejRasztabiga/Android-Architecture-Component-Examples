package pl.rasztabiga.architecturecomponents.books;


import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pl.rasztabiga.architecturecomponents.books.BooksRepository;
import pl.rasztabiga.architecturecomponents.books.persistence.Book;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksDataSource;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksLocalDataSource;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksRemoteDataSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;

public class BooksRepositoryTest {

    private final String BOOK_TITLE1 = "Book1";
    private final long BOOK_NUMBER_OF_PAGES1 = 300L;


    private BooksRepository mBooksRepository;

    @Mock
    private BooksLocalDataSource mBooksLocalDataSource;

    @Mock
    private BooksRemoteDataSource mBooksRemoteDataSource;

    @Mock
    private BooksDataSource.GetBookCallback mGetBooksCallback;

    @Mock
    private BooksDataSource.LoadBooksCallback mLoadBooksCallback;

    @Mock
    private Context mContext;

    @Captor
    private ArgumentCaptor<BooksDataSource.GetBookCallback> mGetBooksCallbackCaptor;

    @Captor
    private ArgumentCaptor<BooksDataSource.LoadBooksCallback> mLoadBooksCallbackCaptor;

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
    public void saveBooks_saveBooksToServiceAPI() {
        Book newBook = new Book(BOOK_TITLE1, BOOK_NUMBER_OF_PAGES1);

        mBooksRepository.saveBook(newBook);

        verify(mBooksRemoteDataSource).saveBook(newBook);
        verify(mBooksLocalDataSource).saveBook(newBook);

        assertThat(mBooksRepository.mCachedBooks.size()).isEqualTo(1);
        
    }

}
