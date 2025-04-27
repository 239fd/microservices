package by.bsuir.employeeservice.mapper;

import by.bsuir.employeeservice.DTO.EmployeeDto;
import by.bsuir.employeeservice.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {
    EmployeeDto toDto(Employee employee);
    Employee toEntity(EmployeeDto dto);
}
