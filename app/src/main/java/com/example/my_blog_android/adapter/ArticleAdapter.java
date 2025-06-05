package com.example.my_blog_android.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.my_blog_android.R;
import com.example.my_blog_android.model.Article;
import com.example.my_blog_android.ui.ArticleDetailActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

// 独立的 ArticleAdapter 类
public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {
    private List<Article> articles;

    // START: Added for long-press functionality
    private OnItemLongClickListener onItemLongClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, Article article);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
    // END: Added for long-press functionality
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public ArticleAdapter(List<Article> articles) {
        this.articles = articles;
    }

    public void updateData(List<Article> newArticles) {
        this.articles.clear();
        this.articles.addAll(newArticles);
        notifyDataSetChanged();
    }

    // START: Added for deletion
    public void removeItem(int position) {
        if (position >= 0 && position < articles.size()) {
            articles.remove(position);
            notifyItemRemoved(position);
        }
    }
    // END: Added for deletion

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        Article article = articles.get(position);
        holder.tvTitle.setText(article.getTitle());
        if (article.getAuthorName() != null && !article.getAuthorName().isEmpty()) {
            holder.tvAuthor.setText("作者: " + article.getAuthorName());
        } else {
            holder.tvAuthor.setText("作者ID: " + article.getAuthorId());
        }
        holder.tvContentPreview.setText(article.getContent());
        if (article.getCreateTime() != null) {
            holder.tvTime.setText("发布时间: " + dateFormat.format(article.getCreateTime()));
        } else {
            holder.tvTime.setText("发布时间: N/A");
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long clickedArticleId = article.getId();
                Intent intent = new Intent(v.getContext(), ArticleDetailActivity.class);
                intent.putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, clickedArticleId);
                v.getContext().startActivity(intent);
            }
        });

        // START: Added for long-press functionality
        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(position, article);
                return true; // Consume the long click
            }
            return false;
        });
        // END: Added for long-press functionality
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    // ViewHolder 类也从 MainActivity 中移到这里
    static class ArticleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvContentPreview, tvTime;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_article_title);
            tvAuthor = itemView.findViewById(R.id.tv_article_author);
            tvContentPreview = itemView.findViewById(R.id.tv_article_content_preview);
            tvTime = itemView.findViewById(R.id.tv_article_time);
        }
    }
}
