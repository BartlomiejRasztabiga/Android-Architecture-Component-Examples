package pl.rasztabiga.architecturecomponents.books.persistence;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

//TODO Add lombok
@Entity(tableName = "books")
public final class Book {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "book_id")
    private final Long id;

    @Nullable
    @ColumnInfo(name = "title")
    private final String title;

    @Nullable
    @ColumnInfo(name = "pages")
    private final Long pages;

    @ColumnInfo(name = "completed")
    private boolean completed;

    @Ignore
    public Book() {
        id = 0L;
        title = "";
        pages = 0L;
    }

    @Ignore
    public Book(@Nullable String title, @Nullable Long pages) {
        this.id = 0L;
        this.title = title;
        this.pages = pages;
    }

    @Ignore
    public Book(@NonNull Long id, @Nullable String title, @Nullable Long pages) {
        this.id = id;
        this.title = title;
        this.pages = pages;
        completed = false;
    }

    public Book(@NonNull Long id, @Nullable String title, @Nullable Long pages,
                boolean completed) {
        this.id = id;
        this.title = title;
        this.pages = pages;
        this.completed = completed;
    }

    @NonNull
    public Long getId() {
        return id;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public Long getPages() {
        return pages;
    }

    @NonNull
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isEmpty() {
        return Strings.isNullOrEmpty(title) &&
                (pages == null || pages == 0L);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book task = (Book) o;
        return Objects.equal(id, task.id) &&
                Objects.equal(title, task.title) &&
                Objects.equal(pages, task.pages);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, title, pages);
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", pages=" + pages +
                ", completed=" + completed +
                '}';
    }
}
