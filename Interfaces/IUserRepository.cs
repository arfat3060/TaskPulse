using TaskPulse.Model;

namespace TaskPulse.Interfaces;

public interface IUserRepository
{
    public Task<IEnumerable<TaskInfo>> GetAllUserDataAsync();
}