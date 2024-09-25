package hello.itemservice.repository.mybatis;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ItemMapper {

    void save(Item item);

    void update(@Param("id") Long id, @Param("updateParam") ItemUpdateDto updateParam);

    // annotation사용 가능 대신 xml에 정의되어 있지 않아야 함
//    @Select("select id, item_name, price, quantity from item where id=#{id}")
    Optional<Item> findById(Long id);

    List<Item> findAll(ItemSearchCond itemSearch);
}
