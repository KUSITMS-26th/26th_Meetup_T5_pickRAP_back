package pickRAP.server.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pickRAP.server.common.BaseException;
import pickRAP.server.common.BaseExceptionStatus;
import pickRAP.server.common.URLPreview;
import pickRAP.server.controller.dto.category.CategoryContentsResponse;
import pickRAP.server.controller.dto.category.CategoryRequest;
import pickRAP.server.controller.dto.category.CategoryResponse;
import pickRAP.server.controller.dto.category.CategoryScrapResponse;
import pickRAP.server.controller.dto.scrap.ScrapResponse;
import pickRAP.server.domain.category.Category;
import pickRAP.server.domain.member.Member;
import pickRAP.server.domain.scrap.Scrap;
import pickRAP.server.domain.scrap.ScrapType;
import pickRAP.server.repository.category.CategoryRepository;
import pickRAP.server.repository.member.MemberRepository;
import pickRAP.server.repository.scrap.ScrapRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pickRAP.server.domain.scrap.QScrap.scrap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final ScrapRepository scrapRepository;

    private final MemberRepository memberRepository;

    @Transactional
    public void initial(Member member) {
        Category category = Category.builder()
                .name("미분류 카테고리")
                .build();
        category.setMember(member);

        categoryRepository.save(category);
    }

    @Transactional
    public CategoryResponse save(CategoryRequest categoryRequest, String email) {
        if(!StringUtils.isEmpty(categoryRequest.getName()) && categoryRequest.getName().length() > 20) {
            throw new BaseException(BaseExceptionStatus.CATEGORY_TITLE_LONG);
        }
        if(categoryRepository.findMemberCategory(categoryRequest.getName(), email).isPresent()) {
            throw new BaseException(BaseExceptionStatus.EXIST_CATEGORY);
        }

        Category category = Category.builder()
                .name(categoryRequest.getName())
                .build();
        category.setMember(memberRepository.findByEmail(email).orElseThrow());
        categoryRepository.save(category);

        return new CategoryResponse(category.getId(), category.getName());
    }

    public List<CategoryResponse> findMemberCategories(String email) {
        Member findMember = memberRepository.findByEmail(email).orElseThrow();

        List<Category> result = categoryRepository.findMemberCategories(findMember);

        return result.stream().map(c -> new CategoryResponse(c.getId(), c.getName())).collect(Collectors.toList());
    }

    public List<CategoryScrapResponse> findMemberCategoriesScrap(String email) {
        Member findMember = memberRepository.findByEmail(email).orElseThrow();

        List<Category> result = categoryRepository.findMemberCategories(findMember);
        List<CategoryScrapResponse> categoryScrapResponses = new ArrayList<>();

        for(Category category : result) {
            if(category.getScraps().isEmpty()) {
                categoryScrapResponses.add(CategoryScrapResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build());
            } else {
                Scrap scrap = category.getScraps().get(category.getScraps().size() - 1);

                categoryScrapResponses.add(CategoryScrapResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .scrapType(scrap.getScrapType())
                        .content(scrap.getContent())
                        .fileUrl(scrap.getFileUrl())
                        .build());
            }
        }

        return categoryScrapResponses;
    }

    @Transactional
    public void update(CategoryRequest categoryRequest, Long id, String email) {
        if(!StringUtils.isEmpty(categoryRequest.getName()) && categoryRequest.getName().length() > 20) {
            throw new BaseException(BaseExceptionStatus.CATEGORY_TITLE_LONG);
        }

        Category findCategory = categoryRepository.findById(id).orElseThrow();

        if(findCategory.getName().equals(categoryRequest.getName())) {
            throw new BaseException(BaseExceptionStatus.SAME_CATEGORY);
        }
        if(categoryRepository.findMemberCategory(categoryRequest.getName(), email).isPresent()) {
            throw new BaseException(BaseExceptionStatus.EXIST_CATEGORY);
        }

        findCategory.updateName(categoryRequest.getName());
    }

    @Transactional
    public void delete(Long id, String email) {
        if(categoryRepository.findById(id).isEmpty()) {
            throw new BaseException(BaseExceptionStatus.DONT_EXIST_CATEGORY);
        }
        if(categoryRepository.findById(id).orElseThrow().getName().equals("미분류 카테고리")) {
            throw new BaseException(BaseExceptionStatus.CANT_DELETE_CATE);
        }

        Category category = categoryRepository.findMemberCategory("미분류 카테고리", email).orElseThrow();
        for(Scrap scrap : categoryRepository.findById(id).orElseThrow().getScraps()) {
            scrap.setCategory(category);
        }

        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CategoryContentsResponse> findMemberCategoriesAllScrap(String email) {
        Member findMember = memberRepository.findByEmail(email).orElseThrow();

        List<Category> result = categoryRepository.findMemberCategories(findMember);

        List<CategoryContentsResponse> categoryContentsResponse = new ArrayList<>();

        for(Category category : result) {
            List<CategoryContentsResponse.ScrapResponse> scrapResponse = new ArrayList<>();

            if(!category.getScraps().isEmpty()) {
                List<Scrap> scrapList = category.getScraps();
                scrapList.forEach(s-> {
                    if(s.getScrapType() == ScrapType.IMAGE
                        || s.getScrapType() == ScrapType.VIDEO
                        || s.getScrapType() == ScrapType.PDF) {

                        scrapResponse.add(CategoryContentsResponse.ScrapResponse.builder()
                                .scrapId(s.getId())
                                .fileUrl(s.getFileUrl())
                                .scrapType(s.getScrapType())
                                .category(s.getCategory().getName())
                                .build());
                    } else if(s.getScrapType() == ScrapType.LINK) {
                        scrapResponse.add(CategoryContentsResponse.ScrapResponse.builder()
                                .scrapId(s.getId())
                                .content(s.getContent())
                                .urlPreview(URLPreview.getLinkPreviewInfo(s.getContent()))
                                .scrapType(s.getScrapType())
                                .category(s.getCategory().getName())
                                .build());
                    } else {
                        scrapResponse.add(CategoryContentsResponse.ScrapResponse.builder()
                                .scrapId(s.getId())
                                .content(s.getContent())
                                .scrapType(s.getScrapType())
                                .category(s.getCategory().getName())
                                .build());
                    }
                });
            }
            categoryContentsResponse.add(
                    CategoryContentsResponse.builder()
                            .categoryId(category.getId())
                            .name(category.getName())
                            .scrapResponseList(scrapResponse)
                            .build());
        }

        return categoryContentsResponse;
    }
}
