package kr.co.sist.login;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.stereotype.Repository;

import kr.co.sist.corp.dto.CorpDTO;
import kr.co.sist.corp.dto.CorpEntity;

@Repository
public interface CorpRepository extends JpaRepository<CorpEntity, Long> {



}
