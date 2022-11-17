package pickRAP.server.service.magazine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pickRAP.server.common.BaseException;
import pickRAP.server.common.BaseExceptionStatus;
import pickRAP.server.controller.dto.magazine.*;
import pickRAP.server.domain.magazine.Magazine;
import pickRAP.server.domain.magazine.MagazinePage;
import pickRAP.server.domain.member.Member;
import pickRAP.server.domain.scrap.Scrap;
import pickRAP.server.repository.magazine.MagazinePageRepository;
import pickRAP.server.repository.magazine.MagazineRepository;
import pickRAP.server.repository.magazine.MagazineRepositoryCustom;
import pickRAP.server.repository.member.MemberRepository;
import pickRAP.server.repository.scrap.ScrapRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MagazineService {

    final static int MAX_TEXT_LENGTH = 400;

    private final MemberRepository memberRepository;
    private final MagazineRepository magazineRepository;
    private final MagazineRepositoryCustom magazineRepositoryCustom;
    private final MagazinePageRepository magazinePageRepository;
    private final ScrapRepository scrapRepository;

    @Transactional
    public void save(MagazineRequest request, String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow();

        Scrap cover = scrapRepository.findById(request.getMagazineCover().getScrapId()).orElseThrow();

        Magazine magazine = Magazine.builder()
                .title(request.getTitle())
                .openStatus(request.isOpenStatus())
                .member(member)
                .cover(cover.getFileUrl())
                .build();

        saveMagazinePages(request.getPageList(), magazine);

        return;
    }

    @Transactional(readOnly = true)
    public List<MagazineListResponse> findMagazines(String email) {
        List<Magazine> findMagazines = magazineRepositoryCustom.findMemberMagazines(email);

        List<MagazineListResponse> collect = findMagazines.stream()
                .map(m -> MagazineListResponse.builder()
                        .magazineId(m.getId())
                        .magazineCover(m.getCover())
                        .title(m.getTitle())
                        .build())
                .collect(Collectors.toList());

        return collect;
    }

    @Transactional(readOnly = true)
    public MagazineResponse findMagazine(Long magazineId) {
        Magazine findMagazine = magazineRepository.findById(magazineId).orElseThrow();
        List<MagazinePage> findMagazinePages = findMagazine.getPages();

        List<MagazinePageResponse> magazinePages = findMagazinePages.stream()
                .map(p -> new MagazinePageResponse(p.getId(), p.getText()))
                .collect(Collectors.toList());

        MagazineResponse magazine = new MagazineResponse(
                findMagazine.getId(), findMagazine.getTitle(), findMagazine.isOpenStatus(),
                findMagazine.getCreateTime(), magazinePages);

        return magazine;
    }

    @Transactional
    public void updateMagazine(MagazineRequest request, Long magazineId, String email) {
        Magazine findMagazine = magazineRepository.findById(magazineId).orElseThrow();

        checkMatchWriter(findMagazine, email);

        Scrap cover = scrapRepository.findById(request.getMagazineCover().getScrapId()).orElseThrow();

        findMagazine.updateMagazine(request.getTitle(), request.isOpenStatus(), cover.getFileUrl());

        magazinePageRepository.deleteByMagazineId(findMagazine.getId());

        saveMagazinePages(request.getPageList(), findMagazine);

        return;
    }

    @Transactional
    public void saveMagazinePages(List<MagazinePageRequest> requestList, Magazine magazine) {
        requestList.forEach(p -> {
            if (p.getText().length() > MAX_TEXT_LENGTH) {
                throw new BaseException(BaseExceptionStatus.EXCEED_TEXT_LENGTH);
            }

            Scrap scrap = scrapRepository.findById(p.getScrapId()).orElseThrow();

            MagazinePage page = MagazinePage.builder()
                    .scrap(scrap)
                    .text(p.getText())
                    .magazine(magazine)
                    .build();

            magazinePageRepository.save(page);
        });

        magazineRepository.save(magazine);
    }

    public void checkMatchWriter(Magazine magazine, String email) {
        if(!magazine.checkWriter(email)) {
            new BaseException(BaseExceptionStatus.NOT_MATCH_WRITER);
        }
    }

    @Transactional
    public void deleteMagazine(Long magazineId, String email) {
        Magazine findMagazine = magazineRepository.findById(magazineId).orElseThrow();

        checkMatchWriter(findMagazine, email);

        magazineRepository.delete(findMagazine);
    }

}
